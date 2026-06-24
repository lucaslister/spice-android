package com.undatech.opaque.ui.data

import org.json.JSONObject

/**
 * A recently launched VM/CT session, shown in the "Recent" strip on the home screen so the
 * user can re-connect in one tap. Holds just enough to rebuild a [VmEntry] and find its
 * [com.undatech.opaque.ui.model.ProxmoxServer] again.
 */
data class RecentSession(
    val serverId: String,
    val serverName: String,
    val vmid: String,
    val name: String,
    val node: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    /** Identity for de-duplication: the same VM on the same server. */
    val key: String get() = "$serverId/$vmid"

    fun toVmEntry(): VmEntry = VmEntry(
        vmid = vmid,
        name = name,
        node = node,
        type = type,
        status = "running",
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("serverId", serverId)
        put("serverName", serverName)
        put("vmid", vmid)
        put("name", name)
        put("node", node)
        put("type", type)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(o: JSONObject): RecentSession = RecentSession(
            serverId = o.optString("serverId"),
            serverName = o.optString("serverName"),
            vmid = o.optString("vmid"),
            name = o.optString("name"),
            node = o.optString("node"),
            type = o.optString("type"),
            timestamp = o.optLong("timestamp", 0L),
        )
    }
}
