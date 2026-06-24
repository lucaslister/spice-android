package com.undatech.opaque.ui.model

import org.json.JSONObject
import java.util.UUID

/**
 * A saved Proxmox VE server. The user saves the host and login once, then browses the
 * server's VMs/containers and SPICEs into any of them. Persisted (encrypted) by
 * [com.undatech.opaque.ui.data.ServerStore]; the password is only stored when [savePassword]
 * is true.
 */
data class ProxmoxServer(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val host: String = "",
    val port: Int = DEFAULT_PORT,
    val user: String = "",
    val realm: String = DEFAULT_REALM,
    val savePassword: Boolean = false,
    val password: String = "",
    val sslStrict: Boolean = false,
    val caCertPem: String = "",
) {
    /** Display name shown in the list; falls back to host when no label is set. */
    val displayName: String
        get() = label.ifBlank { host }

    /** Username in Proxmox's {@code user@realm} form, as the API and ProxmoxClient expect. */
    val userAtRealm: String
        get() = if (user.contains("@")) user else "$user@$realm"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("host", host)
        put("port", port)
        put("user", user)
        put("realm", realm)
        put("savePassword", savePassword)
        put("password", if (savePassword) password else "")
        put("sslStrict", sslStrict)
        put("caCertPem", caCertPem)
    }

    companion object {
        const val DEFAULT_PORT = 8006
        const val DEFAULT_REALM = "pam"

        fun fromJson(o: JSONObject): ProxmoxServer = ProxmoxServer(
            id = o.optString("id", UUID.randomUUID().toString()),
            label = o.optString("label", ""),
            host = o.optString("host", ""),
            port = o.optInt("port", DEFAULT_PORT),
            user = o.optString("user", ""),
            realm = o.optString("realm", DEFAULT_REALM),
            savePassword = o.optBoolean("savePassword", false),
            password = o.optString("password", ""),
            sslStrict = o.optBoolean("sslStrict", false),
            caCertPem = o.optString("caCertPem", ""),
        )
    }
}
