package com.undatech.opaque.ui.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.undatech.opaque.ui.model.ProxmoxServer
import org.json.JSONArray

/**
 * Persists the list of saved [ProxmoxServer]s. The servers (including credentials) are serialized
 * to JSON and encrypted with an Android Keystore backed AES-256-GCM key via [KeystoreCrypto], so
 * credentials are encrypted at rest. On API < 23 (no Keystore AES) it falls back to storing the
 * JSON unencrypted so the app still functions on very old devices.
 */
class ServerStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun getServers(): List<ProxmoxServer> {
        val stored = prefs.getString(KEY_SERVERS, null) ?: return emptyList()
        return runCatching {
            val json = if (KeystoreCrypto.isSupported) KeystoreCrypto.decrypt(stored) else stored
            val arr = JSONArray(json)
            (0 until arr.length()).map { ProxmoxServer.fromJson(arr.getJSONObject(it)) }
        }.getOrElse {
            Log.e(TAG, "Failed to read saved servers", it)
            emptyList()
        }
    }

    fun getServer(id: String): ProxmoxServer? = getServers().firstOrNull { it.id == id }

    /** Adds a new server or replaces the existing one with the same id. */
    fun upsert(server: ProxmoxServer) {
        persist(getServers().filterNot { it.id == server.id } + server)
    }

    fun delete(id: String) {
        persist(getServers().filterNot { it.id == id })
    }

    private fun persist(servers: List<ProxmoxServer>) {
        val arr = JSONArray()
        servers.forEach { arr.put(it.toJson()) }
        val json = arr.toString()
        val stored = runCatching {
            if (KeystoreCrypto.isSupported) KeystoreCrypto.encrypt(json) else json
        }.getOrElse {
            Log.e(TAG, "Encryption failed, storing without it", it)
            json
        }
        prefs.edit().putString(KEY_SERVERS, stored).apply()
    }

    private companion object {
        const val TAG = "ServerStore"
        const val PREFS_FILE = "proxmox_servers"
        const val KEY_SERVERS = "servers"
    }
}
