package org.microg.gms.constellation.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.google.android.gms.iid.InstanceID
import com.squareup.wire.Instant
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

internal const val CONSTELLATION_EC_PREFS = "constellation_prefs"
internal const val KEY_EC_PRIVATE = "private_key"
internal const val KEY_EC_PUBLIC = "public_key"
internal const val KEY_PUBLIC_KEY_ACKED = "is_public_key_acked"

private const val TAG = "C11NAuthManager"

val Context.authManager: AuthManager get() = AuthManager.get(this)

internal fun requireIidToken(token: String?): String {
    if (token.isNullOrEmpty()) {
        throw IllegalStateException("IID token is empty")
    }
    return token
}

internal fun decodeEcKeyPair(privateKeyStr: String, publicKeyStr: String): KeyPair {
    val kf = KeyFactory.getInstance("EC")
    val privateKey = kf.generatePrivate(
        PKCS8EncodedKeySpec(Base64.decode(privateKeyStr, Base64.DEFAULT))
    )
    val publicKey = kf.generatePublic(
        X509EncodedKeySpec(Base64.decode(publicKeyStr, Base64.DEFAULT))
    )
    return KeyPair(publicKey, privateKey)
}

internal fun loadOrCreateEcKeyPair(prefs: SharedPreferences): KeyPair {
    val privateKeyStr = prefs.getString(KEY_EC_PRIVATE, null)
    val publicKeyStr = prefs.getString(KEY_EC_PUBLIC, null)

    if (privateKeyStr != null && publicKeyStr != null) {
        try {
            return decodeEcKeyPair(privateKeyStr, publicKeyStr)
        } catch (e: Exception) {
            Log.w(TAG, "Stored EC keys are corrupt; regenerating", e)
        }
    }

    val kpg = KeyPairGenerator.getInstance("EC")
    kpg.initialize(256)
    val kp = kpg.generateKeyPair()

    // Commit so the next GPNV/Sync cannot still read a stale public-key ack.
    prefs.edit(commit = true) {
        putString(KEY_EC_PRIVATE, Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
        putString(KEY_EC_PUBLIC, Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP))
        putBoolean(KEY_PUBLIC_KEY_ACKED, false)
    }
    Log.w(TAG, "Regenerated EC key pair; public-key ack reset")
    return kp
}

class AuthManager private constructor(context: Context) {
    private val context = context.applicationContext
    private val sharedPrefs = context.getSharedPreferences(CONSTELLATION_EC_PREFS, Context.MODE_PRIVATE)

    companion object {
        // This is safe as the Context is immediately converted to the application context.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: AuthManager? = null

        fun get(context: Context): AuthManager = instance ?: synchronized(this) {
            instance ?: AuthManager(context).also { instance = it }
        }
    }

    // GMS signing format: {iidToken}:{seconds}:{nanos}
    fun signIidTokenCompat(iidToken: String): Pair<ByteArray, Long> {
        val currentTimeMillis = System.currentTimeMillis()

        val epochSecond = currentTimeMillis / 1000
        val nano = (currentTimeMillis % 1000) * 1_000_000

        val content = "$iidToken:$epochSecond:$nano"
        return sign(content) to currentTimeMillis
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun signIidToken(iidToken: String): Pair<ByteArray, Instant> {
        val (bytes, millis) = signIidTokenCompat(iidToken)
        return bytes to Instant.ofEpochMilli(millis)
    }

    fun getIidToken(projectNumber: String? = null): String {
        val sender = projectNumber ?: IidTokenPhenotypes.DEFAULT_PROJECT_NUMBER
        return requireIidToken(InstanceID.getInstance(context).getToken(sender, "GCM"))
    }

    fun getOrCreateKeyPair(): KeyPair = loadOrCreateEcKeyPair(sharedPrefs)

    fun sign(content: String): ByteArray {
        return try {
            val kp = getOrCreateKeyPair()
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(kp.private)
            signature.update(content.toByteArray(StandardCharsets.UTF_8))
            signature.sign()
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    fun getFid(): String = InstanceID.getInstance(context).id
}
