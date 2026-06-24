package com.undatech.opaque.ui.servers

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.undatech.opaque.ui.data.AppPreferences
import com.undatech.opaque.ui.data.RecentSession
import com.undatech.opaque.ui.data.ServerStore
import com.undatech.opaque.ui.model.ProxmoxServer
import com.undatech.opaque.ui.session.SessionLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServerListViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val prefs = AppPreferences(app)

    private val _servers = MutableStateFlow<List<ProxmoxServer>>(emptyList())
    val servers: StateFlow<List<ProxmoxServer>> = _servers.asStateFlow()

    private val _recents = MutableStateFlow<List<RecentSession>>(emptyList())
    val recents: StateFlow<List<RecentSession>> = _recents.asStateFlow()

    init {
        refresh()
    }

    /** Reloads servers and recents; called on first composition and on every resume. */
    fun refresh() {
        val servers = store.getServers().sortedBy { it.displayName.lowercase() }
        _servers.value = servers
        prefs.pruneRecents(servers.map { it.id }.toSet())
        _recents.value = prefs.getRecents()
    }

    fun delete(server: ProxmoxServer) {
        store.delete(server.id)
        refresh()
    }

    /** Re-launches a recent session, reusing the saved password when available. */
    fun launchRecent(context: Context, recent: RecentSession) {
        val server = store.getServer(recent.serverId) ?: return
        val password = if (server.savePassword) server.password else ""
        SessionLauncher.launch(context, server, password, recent.toVmEntry())
    }
}
