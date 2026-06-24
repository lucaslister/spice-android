package com.undatech.opaque

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.undatech.opaque.proxmox.ProxmoxClient
import com.undatech.opaque.proxmox.pojo.PveResource
import com.undatech.remoteClientUi.R
import java.util.concurrent.Executors

class VmPickerBottomSheet : BottomSheetDialogFragment() {

    interface OnVmSelectedListener {
        fun onVmSelected(vmId: String, vmName: String)
    }

    private var listener: OnVmSelectedListener? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnVmSelectedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_vm_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progress = view.findViewById<CircularProgressIndicator>(R.id.vmPickerProgress)
        val errorText = view.findViewById<TextView>(R.id.vmPickerError)
        val recycler = view.findViewById<RecyclerView>(R.id.vmPickerList)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        val hostname = arguments?.getString(ARG_HOSTNAME) ?: return
        val username = arguments?.getString(ARG_USERNAME) ?: return
        val password = arguments?.getString(ARG_PASSWORD) ?: return

        executor.execute {
            try {
                val conn = ConnectionSettings(java.util.UUID.randomUUID().toString())
                conn.hostname = hostname
                conn.setUser(username)
                conn.setPassword(password)

                val client = ProxmoxClient(conn, null, null)
                val realms = client.getAvailableRealms()
                val realm = parseRealm(username, realms.keys)
                val user = parseUser(username)
                client.login(user, realm, password, null)

                val allResources = client.getResources()
                val vms = allResources.values
                    .filter { it.type == "qemu" || it.type == "lxc" }
                    .sortedBy { it.name ?: it.vmid }

                if (!isAdded) return@execute
                requireActivity().runOnUiThread {
                    progress.visibility = View.GONE
                    if (vms.isEmpty()) {
                        errorText.text = getString(R.string.proxmox_browse_error, "No VMs found")
                        errorText.visibility = View.VISIBLE
                    } else {
                        recycler.adapter = VmAdapter(vms) { resource ->
                            val vmId = resource.vmid
                            val vmName = resource.name ?: vmId
                            listener?.onVmSelected(vmId, vmName)
                            dismiss()
                        }
                        recycler.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                if (!isAdded) return@execute
                requireActivity().runOnUiThread {
                    progress.visibility = View.GONE
                    errorText.text = getString(R.string.proxmox_browse_error, e.message ?: "Unknown error")
                    errorText.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun parseRealm(username: String, knownRealms: Set<String>): String {
        val atIndex = username.lastIndexOf('@')
        if (atIndex >= 0) {
            val candidate = username.substring(atIndex + 1)
            if (knownRealms.contains(candidate)) return candidate
        }
        return knownRealms.firstOrNull() ?: "pam"
    }

    private fun parseUser(username: String): String {
        val atIndex = username.lastIndexOf('@')
        return if (atIndex >= 0) username.substring(0, atIndex) else username
    }

    private inner class VmAdapter(
        private val items: List<PveResource>,
        private val onClick: (PveResource) -> Unit
    ) : RecyclerView.Adapter<VmAdapter.VmViewHolder>() {

        inner class VmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.vm_name)
            val detail: TextView = view.findViewById(R.id.vm_detail)
            val status: Chip = view.findViewById(R.id.vm_status)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VmViewHolder =
            VmViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_vm, parent, false))

        override fun onBindViewHolder(holder: VmViewHolder, position: Int) {
            val resource = items[position]
            holder.name.text = resource.name ?: resource.vmid
            holder.detail.text = "${resource.node} · ${resource.type}"
            holder.status.text = getString(R.string.proxmox_vm_status_stopped)
            holder.itemView.setOnClickListener { onClick(resource) }
        }

        override fun getItemCount() = items.size
    }

    companion object {
        private const val ARG_HOSTNAME = "hostname"
        private const val ARG_USERNAME = "username"
        private const val ARG_PASSWORD = "password"

        @JvmStatic
        fun newInstance(hostname: String, username: String, password: String): VmPickerBottomSheet =
            VmPickerBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_HOSTNAME, hostname)
                    putString(ARG_USERNAME, username)
                    putString(ARG_PASSWORD, password)
                }
            }
    }
}
