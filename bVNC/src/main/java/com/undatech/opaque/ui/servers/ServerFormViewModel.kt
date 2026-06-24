package com.undatech.opaque.ui.servers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undatech.opaque.ui.data.ProxmoxRepository
import com.undatech.opaque.ui.data.ProxmoxResult
import com.undatech.opaque.ui.data.ServerStore
import com.undatech.opaque.ui.model.ProxmoxServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerFormState(
    val id: String? = null,
    val label: String = "",
    val host: String = "",
    val port: String = ProxmoxServer.DEFAULT_PORT.toString(),
    val user: String = "",
    val realm: String = ProxmoxServer.DEFAULT_REALM,
    val password: String = "",
    val savePassword: Boolean = true,
    val sslStrict: Boolean = false,
    val isEditing: Boolean = false,
    val testing: Boolean = false,
    val testMessage: String? = null,
    val testSuccess: Boolean = false,
)

class ServerFormViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val repository = ProxmoxRepository(app)

    private val _state = MutableStateFlow(ServerFormState())
    val state: StateFlow<ServerFormState> = _state.asStateFlow()

    private var loaded = false

    fun load(serverId: String?) {
        if (loaded) return
        loaded = true
        val existing = serverId?.let { store.getServer(it) }
        if (existing != null) {
            _state.value = ServerFormState(
                id = existing.id,
                label = existing.label,
                host = existing.host,
                port = existing.port.toString(),
                user = existing.user,
                realm = existing.realm,
                password = existing.password,
                savePassword = existing.savePassword,
                sslStrict = existing.sslStrict,
                isEditing = true,
            )
        }
    }

    fun onLabel(v: String) = _state.update { it.copy(label = v, testMessage = null) }
    fun onHost(v: String) = _state.update { it.copy(host = v, testMessage = null) }
    fun onPort(v: String) = _state.update { it.copy(port = v.filter(Char::isDigit), testMessage = null) }
    fun onUser(v: String) = _state.update { it.copy(user = v, testMessage = null) }
    fun onRealm(v: String) = _state.update { it.copy(realm = v, testMessage = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, testMessage = null) }
    fun onSavePassword(v: Boolean) = _state.update { it.copy(savePassword = v) }
    fun onSslStrict(v: Boolean) = _state.update { it.copy(sslStrict = v) }

    fun isValid(): Boolean = with(_state.value) {
        host.isNotBlank() && user.isNotBlank()
    }

    fun toServer(): ProxmoxServer = with(_state.value) {
        ProxmoxServer(
            id = id ?: ProxmoxServer().id,
            label = label.trim(),
            host = host.trim(),
            port = port.toIntOrNull() ?: ProxmoxServer.DEFAULT_PORT,
            user = user.trim(),
            realm = realm.trim().ifBlank { ProxmoxServer.DEFAULT_REALM },
            savePassword = savePassword,
            password = if (savePassword) password else "",
            sslStrict = sslStrict,
        )
    }

    fun save(): ProxmoxServer {
        val server = toServer()
        store.upsert(server)
        return server
    }

    /** Authenticates against the server to confirm host/credentials before saving. */
    fun testConnection() {
        val server = toServer()
        _state.update { it.copy(testing = true, testMessage = null, testSuccess = false) }
        viewModelScope.launch {
            val result = repository.listVms(server, _state.value.password, otp = null)
            val (msg, ok) = when (result) {
                is ProxmoxResult.Success ->
                    "Connected — ${result.vms.size} VM(s)/container(s) found" to true
                is ProxmoxResult.NeedsOtp ->
                    "Credentials OK (this realm requires a 2FA code at connect time)" to true
                is ProxmoxResult.AuthFailed -> "Authentication failed: ${result.message}" to false
                is ProxmoxResult.Error -> "Could not connect: ${result.message}" to false
            }
            _state.update { it.copy(testing = false, testMessage = msg, testSuccess = ok) }
        }
    }
}
