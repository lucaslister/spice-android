package com.iiordanov.bVNC.protocol

import android.content.Context
import com.iiordanov.bVNC.Utils
import com.undatech.opaque.Connection
import com.undatech.opaque.Viewable
import com.undatech.remoteClientUi.R

class RemoteConnectionFactory(
    val context: Context,
    val connection: Connection?,
    val viewable: Viewable,
    private val hideKeyboardAndExtraKeys: Runnable,
) {
    // This flag indicates whether this is the VNC client
    private var isVnc = Utils.isVnc(context)

    // This flag indicates whether this is the RDP client
    private var isRdp = Utils.isRdp(context)

    // This flag indicates whether this is the SPICE client
    private var isSpice = Utils.isSpice(context)

    // This flag indicates whether this is the Opaque client
    private var isOpaque = Utils.isOpaque(context)

    // This flag indicates whether Opaque is connecting to oVirt
    private var isOvirt =
        connection?.connectionTypeString == context.resources.getString(R.string.connection_type_ovirt)

    // This flag indicates whether Opaque is connecting to Proxmox
    private var isProxmox =
        connection?.connectionTypeString == context.resources.getString(R.string.connection_type_pve)

    fun build(): RemoteConnection {
        // A Proxmox/oVirt connection type wins regardless of build flavor: it is only ever set
        // on a ConnectionSettings produced by the hypervisor UI, so this lets the SPICE-flavored
        // app drive the Proxmox flow (browse VMs -> fetch .vv -> SPICE) deterministically.
        return when {
            isProxmox -> RemoteProxmoxConnection(context, connection, viewable, hideKeyboardAndExtraKeys)
            isOvirt -> RemoteOvirtConnection(context, connection, viewable, hideKeyboardAndExtraKeys)
            isSpice -> RemoteSpiceConnection(context, connection, viewable, hideKeyboardAndExtraKeys)
            isRdp -> RemoteRdpConnection(context, connection, viewable, hideKeyboardAndExtraKeys)
            isVnc -> RemoteVncConnection(context, connection, viewable, hideKeyboardAndExtraKeys)
            isOpaque -> RemoteSpiceConnection(context, connection, viewable, hideKeyboardAndExtraKeys)
            else -> throw IllegalStateException("App type must be one of VNC, RDP, SPICE or Opaque")
        }
    }
}