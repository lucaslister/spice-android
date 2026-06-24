package com.undatech.opaque.ui.session

import android.content.Context
import android.content.Intent
import com.iiordanov.bVNC.Constants
import com.iiordanov.bVNC.RemoteCanvasActivity
import com.undatech.opaque.ConnectionSettings
import com.undatech.opaque.ui.data.AppPreferences
import com.undatech.opaque.ui.data.RecentSession
import com.undatech.opaque.ui.data.VmEntry
import com.undatech.opaque.ui.model.ProxmoxServer
import com.undatech.remoteClientUi.R

/**
 * Bridges the Compose UI into the existing, unchanged SPICE session pipeline. It builds a
 * [ConnectionSettings] describing the chosen Proxmox VM and starts [RemoteCanvasActivity] the
 * same way IntentHelper does. Because the connection type is "Proxmox VE" and the VM id is set,
 * RemoteConnectionFactory routes to RemoteProxmoxConnection, which authenticates, starts the VM
 * if needed, fetches the .vv file and connects via native SPICE. If the password is not saved
 * (or OTP is required), the existing canvas handler prompts for it.
 */
object SessionLauncher {

    /** A single reused filename keeps any persisted prefs from accumulating per launch. */
    private const val SESSION_FILENAME = "proxmox_session"

    fun launch(context: Context, server: ProxmoxServer, password: String, vm: VmEntry) {
        val cs = ConnectionSettings.newConnectionFromDefaultTemplate(context, SESSION_FILENAME).apply {
            setConnectionTypeString(context.getString(R.string.connection_type_pve))
            hostname = server.host
            setUser(server.userAtRealm)
            setPassword(password)
            keepPassword = password.isNotEmpty()
            vmname = vm.vmid
            isSslStrict = server.sslStrict
            isAudioPlaybackEnabled = false
        }

        AppPreferences(context).addRecent(
            RecentSession(
                serverId = server.id,
                serverName = server.displayName,
                vmid = vm.vmid,
                name = vm.name,
                node = vm.node,
                type = vm.type,
            )
        )

        val intent = Intent(context, RemoteCanvasActivity::class.java).apply {
            putExtra(Constants.opaqueConnectionSettingsClassPath, cs)
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
