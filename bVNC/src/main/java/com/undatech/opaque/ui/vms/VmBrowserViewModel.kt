package com.undatech.opaque.ui.vms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undatech.opaque.ui.data.ProxmoxRepository
import com.undatech.opaque.ui.data.ProxmoxResult
import com.undatech.opaque.ui.data.ServerStore
import com.undatech.opaque.ui.data.VmEntry
import com.undatech.opaque.ui.model.ProxmoxServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VmBrowserUiState {
    data object Loading : VmBrowserUiState
    data class NeedPassword(val serverName: String) : VmBrowserUiState
    data class NeedOtp(val serverName: String) : VmBrowserUiState
    data class Content(val serverName: String, val vms: List<VmEntry>, val refreshing: Boolean) : VmBrowserUiState
    data class Error(val message: String) : VmBrowserUiState
}

class VmBrowserViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServerStore(app)
    private val repository = ProxmoxRepository(app)

    private val _state = MutableStateFlow<VmBrowserUiState>(VmBrowserUiState.Loading)
    val state: StateFlow<VmBrowserUiState> = _state.asStateFlow()

    private var server: ProxmoxServer? = null
    private var password: String = ""
    private var otp: String? = null
    private var loaded = false

    fun load(serverId: String) {
        if (loaded) return
        loaded = true
        val s = store.getServer(serverId)
        if (s == null) {
            _state.value = VmBrowserUiState.Error("Server not found")
            return
        }
        server = s
        password = if (s.savePassword) s.password else ""
        if (password.isEmpty()) {
            _state.value = VmBrowserUiState.NeedPassword(s.displayName)
        } else {
            fetch()
        }
    }

    fun submitPassword(pw: String) {
        password = pw
        fetch()
    }

    fun submitOtp(code: String) {
        otp = code
        fetch()
    }

    fun refresh() = fetch(refreshing = true)

    private fun fetch(refreshing: Boolean = false) {
        val s = server ?: return
        val current = _state.value
        _state.value = when {
            refreshing && current is VmBrowserUiState.Content -> current.copy(refreshing = true)
            else -> VmBrowserUiState.Loading
        }
        viewModelScope.launch {
            when (val result = repository.listVms(s, password, otp)) {
                is ProxmoxResult.Success ->
                    _state.value = VmBrowserUiState.Content(s.displayName, result.vms, refreshing = false)
                is ProxmoxResult.NeedsOtp ->
                    _state.value = VmBrowserUiState.NeedOtp(s.displayName)
                is ProxmoxResult.AuthFailed -> {
                    // Force re-entry of the password on the next attempt.
                    password = ""
                    otp = null
                    _state.value = VmBrowserUiState.Error(result.message)
                }
                is ProxmoxResult.Error ->
                    _state.value = VmBrowserUiState.Error(result.message)
            }
        }
    }

    fun currentServer(): ProxmoxServer? = server
    fun currentPassword(): String = password
}
