package com.undatech.opaque.ui

/** Navigation routes for the Compose front end. */
object Routes {
    const val SERVERS = "servers"
    const val SETTINGS = "settings"

    private const val SERVER_FORM_BASE = "server_form"
    const val SERVER_FORM = "$SERVER_FORM_BASE?serverId={serverId}"
    fun serverForm(serverId: String? = null): String =
        if (serverId == null) SERVER_FORM_BASE else "$SERVER_FORM_BASE?serverId=$serverId"

    const val VM_BROWSER = "vms/{serverId}"
    fun vmBrowser(serverId: String): String = "vms/$serverId"

    const val ARG_SERVER_ID = "serverId"
}
