package org.microg.gms.cryptauth

import android.accounts.Account
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.android.gms.BuildConfig
import com.google.android.gms.common.Scopes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.gcm.GcmConstants
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.RegisterRequest
import org.microg.gms.gcm.completeRegisterRequest
import java.security.KeyPairGenerator
import java.security.MessageDigest

private const val TAG = "CryptAuthFlow"

private const val GCM_REGISTER_SENDER = "16502139086"
private const val GCM_REGISTER_SUBTYPE = "16502139086"
private const val GCM_REGISTER_SUBSCRIPTION = "16502139086"
private const val GCM_REGISTER_SCOPE = "GCM"
private const val GCM_REGISTER_CLIV = "iid-202414000"
private const val GCM_REGISTER_INFO = "0wqs6iYsl_URQEb1aBJ6XhzCbHSr-hg"

private const val RSA_KEY_SIZE = 2048

private const val AFTER_REQUEST_DELAY = 5000L

suspend fun Context.sendDeviceScreenlockState(account: Account): Boolean = sendDeviceScreenlockState(account.name)

suspend fun Context.sendDeviceScreenlockState(accountName: String): Boolean = cryptAuthStateMutex.withLock {
    try {
        sendDeviceScreenlockStateLocked(accountName)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        // Exceptions from authentication, protocol parsing or network services can contain secrets.
        Log.w(TAG, "CryptAuth screen-lock update failed (${failure.javaClass.simpleName})")
        false
    }
}

private suspend fun Context.sendDeviceScreenlockStateLocked(accountName: String): Boolean {

    // Ensure that device is checked in
    val checkinInfo = LastCheckinInfo.read(this)
    if (checkinInfo.androidId == 0L) {
        Log.w(TAG, "Device is not checked in, as it doesn't have an Android ID. Cannot perform cryptauth flow.")
        return false
    }

    // Instance ID token for use in CryptAuth query
    val instanceId = generateAppId()
    val instanceToken = registerForCryptAuth(checkinInfo, instanceId).getString(GcmConstants.EXTRA_REGISTRATION_ID)
    if (instanceToken == null) {
        Log.w(TAG, "No instance ID was gathered. Is GCM enabled, has there been a checkin?")
        return false
    }

    // Auth token for use in CryptAuth query
    val authToken = authenticateForCryptAuth(accountName)
    if (authToken == null) {
        Log.w(TAG, "Authentication failed. Cannot perform cryptauth flow.")
        return false
    }

    val cryptAuthSyncKeysResult = cryptAuthSyncKeys(authToken, instanceId, instanceToken, checkinInfo.androidId)
    if (cryptAuthSyncKeysResult == null
        || !cryptAuthSyncKeysResult.has(CRYPTAUTH_FIELD_SESSION_ID)
        || cryptAuthSyncKeysResult.get(CRYPTAUTH_FIELD_SESSION_ID) !is String
    ) {
        Log.w(TAG, "CryptAuth syncKeys failed. Cannot complete flow.")
        return false
    }

    val session: String = cryptAuthSyncKeysResult.get(CRYPTAUTH_FIELD_SESSION_ID) as String
    val cryptAuthEnrollKeysResult = cryptAuthEnrollKeys(authToken, session)

    return if (cryptAuthEnrollKeysResult != null) {
        /* Give Google server some time to process the new information.
         * This leads to higher success rate compared to sending
         * the next query immediately after this one. Tests show that it
         * makes sense to wait multiple seconds.
         */
        delay(AFTER_REQUEST_DELAY)
        true
    } else {
        false
    }
}

/**
 * Synchronize transfer keys after the transfer service has authorized its caller and obtained
 * account-specific consent. The existing screen-lock error resolver does not invoke this flow.
 */
internal suspend fun Context.ensureCryptAuthTransferKeys(
    account: Account,
    diagnostic: CryptAuthTransferDiagnostics = CryptAuthTransferDiagnostics()
): CryptAuthState = cryptAuthStateMutex.withLock {
    diagnostic.at(CryptAuthTransferDiagnosticReason.STORAGE_API) {
        check(android.os.Build.VERSION.SDK_INT >= 21) { "CryptAuth transfer requires no-backup storage" }
    }
    withContext(Dispatchers.IO) {
        val checkin = diagnostic.at(CryptAuthTransferDiagnosticReason.CHECKIN_READ) {
            LastCheckinInfo.read(this@ensureCryptAuthTransferKeys)
        }
        diagnostic.at(CryptAuthTransferDiagnosticReason.CHECKIN_REQUIRED) {
            check(checkin.androidId != 0L) { "CryptAuth transfer requires device check-in" }
        }
        val store = diagnostic.at(CryptAuthTransferDiagnosticReason.STORAGE_OPEN) {
            CryptAuthKeyStore(this@ensureCryptAuthTransferKeys, account.name)
        }
        val instanceId = diagnostic.at(CryptAuthTransferDiagnosticReason.STATE_READ) { store.read().instanceId }
        val registration = diagnostic.at(CryptAuthTransferDiagnosticReason.GCM_REGISTRATION) {
            registerForCryptAuth(checkin, instanceId)
        }
        val instanceToken = diagnostic.at(CryptAuthTransferDiagnosticReason.GCM_TOKEN) {
            requireNotNull(registration.getString(GcmConstants.EXTRA_REGISTRATION_ID)) { "Missing CryptAuth registration" }
        }
        val authentication = diagnostic.at(CryptAuthTransferDiagnosticReason.OAUTH_AUTHENTICATION) {
            authenticateForCryptAuth(account.name)
        }
        val authToken = diagnostic.at(CryptAuthTransferDiagnosticReason.OAUTH_TOKEN) {
            requireNotNull(authentication) { "CryptAuth authentication failed" }
        }
        val http = CryptAuthHttpTransport(authToken)
        val transport = object : CryptAuthTransport {
            override suspend fun sync(request: cryptauthv2.SyncKeysRequest) =
                diagnostic.at(CryptAuthTransferDiagnosticReason.SYNC_KEYS) { http.sync(request) }
            override suspend fun enroll(request: cryptauthv2.EnrollKeysRequest) =
                diagnostic.at(CryptAuthTransferDiagnosticReason.ENROLL_KEYS) { http.enroll(request) }
        }
        val metadata = diagnostic.at(CryptAuthTransferDiagnosticReason.CLIENT_METADATA) {
            cryptAuthMetadata(instanceId, instanceToken, checkin.androidId)
        }
        diagnostic.at(CryptAuthTransferDiagnosticReason.KEY_ENROLLMENT) {
            CryptAuthEnrollment(transport, store).enroll(metadata)
        }
        val state = diagnostic.at(CryptAuthTransferDiagnosticReason.STATE_RELOAD) { store.read() }
        diagnostic.at(CryptAuthTransferDiagnosticReason.TRANSFER_KEYS) {
            check(state.pendingKeys.isEmpty() && state.keys.count { it.name == "PublicKey" && it.active } == 1 &&
                state.keys.count { it.name == "authzen" && it.active && it.type == cryptauthv2.KeyType.RAW256 } == 1) {
                "CryptAuth transfer keys are unavailable"
            }
        }
        state
    }
}

private suspend fun Context.registerForCryptAuth(checkinInfo: LastCheckinInfo, instanceId: String): Bundle = completeRegisterRequest(
    context = this,
    database = GcmDatabase(this),
    request = RegisterRequest().build(this)
        .checkin(checkinInfo)
        .app("com.google.android.gms", CERTIFICATE.lowercase(), BuildConfig.VERSION_CODE)
        .sender(GCM_REGISTER_SENDER)
        .extraParam("subscription", GCM_REGISTER_SUBSCRIPTION)
        .extraParam("X-subscription", GCM_REGISTER_SUBSCRIPTION)
        .extraParam("subtype", GCM_REGISTER_SUBTYPE)
        .extraParam("X-subtype", GCM_REGISTER_SUBTYPE)
        .extraParam("app_ver", BuildConfig.VERSION_CODE.toString())
        .extraParam("osv", "29")
        .extraParam("cliv", GCM_REGISTER_CLIV)
        .extraParam("gmsv", BuildConfig.VERSION_CODE.toString())
        .extraParam("appid", instanceId)
        .extraParam("scope", GCM_REGISTER_SCOPE)
        .extraParam("app_ver_name","%09d".format(BuildConfig.VERSION_CODE).let {
            "${it.substring(0, 2)}.${it.substring(2, 4)}.${it.substring(4, 6)} (190800-{{cl}})"
        })
        .info(GCM_REGISTER_INFO)
)

private suspend fun Context.authenticateForCryptAuth(accountName: String): String? {
    val cryptAuthServiceOauth2 = AuthConstants.SCOPE_OAUTH2 + Scopes.CRYPTAUTH
    val authManager = AuthManager(this, accountName, Constants.GMS_PACKAGE_NAME, cryptAuthServiceOauth2)
    return withContext(Dispatchers.IO) { authManager.requestAuth(false).auth }
}

/**
 * Generates an app / instance ID based on the hash of the public key of an RSA keypair.
 * The key itself is never used.
 */
fun generateAppId(): String {
    val rsaGenerator = KeyPairGenerator.getInstance("RSA")
    rsaGenerator.initialize(RSA_KEY_SIZE)
    val keyPair = rsaGenerator.generateKeyPair()

    val digest = MessageDigest.getInstance("SHA1").digest(keyPair.public.encoded)
    digest[0] = ((112 + (0xF and digest[0].toInt())) and 0xFF).toByte()
    return Base64.encodeToString(
        digest, 0, 8, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
}

fun Context.isLockscreenConfigured(): Boolean {
    val service: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return if (android.os.Build.VERSION.SDK_INT >= 23) {
        service.isDeviceSecure
    } else {
        service.isKeyguardSecure
    }
}
