package com.undatech.opaque.ui.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Base64
import com.undatech.opaque.ConnectionSettings
import com.undatech.opaque.RemoteClientLibConstants
import com.undatech.opaque.proxmox.ProxmoxClient
import com.undatech.opaque.proxmox.pojo.PveResource
import com.undatech.opaque.ui.model.ProxmoxServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.X509Certificate
import java.util.UUID
import javax.security.auth.login.LoginException

/** A VM or container exposed by a Proxmox server, ready to display and SPICE into. */
data class VmEntry(
    val vmid: String,
    val name: String,
    val node: String,
    val type: String,
    val status: String,
) {
    val isRunning: Boolean get() = status.equals("running", ignoreCase = true)
    val isContainer: Boolean get() = type.equals("lxc", ignoreCase = true)
}

/** Result of a connection/list attempt, so the UI can react to the TFA-required case. */
sealed interface ProxmoxResult {
    data class Success(val vms: List<VmEntry>) : ProxmoxResult
    data class AuthFailed(val message: String) : ProxmoxResult
    data object NeedsOtp : ProxmoxResult
    data class Error(val message: String) : ProxmoxResult
}

/**
 * Thin coroutine wrapper around the existing [ProxmoxClient]. Mirrors the proven login/list
 * sequence in VmPickerBottomSheet (get realms -> parse realm/user -> login -> cluster
 * resources), running off the main thread.
 */
class ProxmoxRepository(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Authenticates and returns the server's VMs/containers. [otp] is supplied on a retry when
     * the realm (or user) requires two-factor auth.
     */
    suspend fun listVms(server: ProxmoxServer, password: String, otp: String?): ProxmoxResult =
        withContext(Dispatchers.IO) {
            try {
                val conn = ConnectionSettings(UUID.randomUUID().toString()).apply {
                    hostname = server.host
                    setUser(server.userAtRealm)
                    setPassword(password)
                    isSslStrict = server.sslStrict
                }

                // RestClient's trust manager blocks on a Handler to get the user to accept an
                // untrusted (self-signed) cert. Pre-session we have no dialog, so supply a handler
                // that auto-accepts on first use and pins the cert onto this throwaway connection.
                val client = ProxmoxClient(conn, certAcceptingHandler(conn), server.caCertPem.ifBlank { null })
                val realms = client.availableRealms
                val realm = resolveRealm(server, realms.keys)
                val user = resolveUser(server.userAtRealm)

                // If the realm advertises TFA and we have no code yet, ask the UI for one.
                if (otp.isNullOrEmpty() && realms[realm]?.tfa != null) {
                    return@withContext ProxmoxResult.NeedsOtp
                }

                client.login(user, realm, password, otp)
                if (client.isPerUserNeedTfa && otp.isNullOrEmpty()) {
                    return@withContext ProxmoxResult.NeedsOtp
                }

                val vms = client.resources.values
                    .filter { it.type == PveResource.Types.QEMU || it.type == PveResource.Types.LXC }
                    .sortedWith(compareBy({ it.name ?: it.vmid }, { it.vmid }))
                    .map {
                        VmEntry(
                            vmid = it.vmid,
                            name = it.name ?: it.vmid,
                            node = it.node ?: "",
                            type = it.type ?: "",
                            status = it.status ?: "unknown",
                        )
                    }
                ProxmoxResult.Success(vms)
            } catch (e: LoginException) {
                ProxmoxResult.AuthFailed(e.message ?: "Authentication failed")
            } catch (e: Exception) {
                ProxmoxResult.Error(e.message ?: "Unknown error")
            }
        }

    /**
     * A Handler that satisfies RestClient's certificate-acceptance protocol without UI: when the
     * trust manager asks (DIALOG_X509_CERT), it pins the presented cert onto [conn] and wakes the
     * blocked network thread. RestClient holds the handler's monitor and waits() on it; we take the
     * same monitor, set the fields its wait-loop checks, and notifyAll(). Runs on the main looper,
     * so it never deadlocks the IO thread that's blocked here.
     */
    private fun certAcceptingHandler(conn: ConnectionSettings): Handler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what != RemoteClientLibConstants.DIALOG_X509_CERT) return
                synchronized(this) {
                    val cert = msg.obj as? X509Certificate
                    conn.x509KeySignature =
                        if (cert != null) Base64.encodeToString(cert.encoded, Base64.DEFAULT)
                        else "accepted"
                    conn.ovirtCaData = "accepted"
                    (this as java.lang.Object).notifyAll()
                }
            }
        }

    private fun resolveRealm(server: ProxmoxServer, knownRealms: Set<String>): String {
        if (server.user.contains("@")) {
            val candidate = server.user.substringAfterLast('@')
            if (knownRealms.contains(candidate)) return candidate
        }
        if (knownRealms.contains(server.realm)) return server.realm
        return knownRealms.firstOrNull() ?: ProxmoxServer.DEFAULT_REALM
    }

    private fun resolveUser(userAtRealm: String): String =
        if (userAtRealm.contains("@")) userAtRealm.substringBeforeLast('@') else userAtRealm
}
