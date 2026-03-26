package org.microg.gms.constellation.core

import android.content.Context
import android.util.Base64
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

class AuthManager(context: Context) {
    private val context = context.applicationContext

    companion object {
        private const val PREFS_NAME = "constellation_prefs"
        private const val KEY_PRIVATE = "private_key"
        private const val KEY_PUBLIC = "public_key"

        @Volatile
        private var instance: AuthManager? = null

        fun get(context: Context): AuthManager {
            val existing = instance
            if (existing != null) return existing

            return synchronized(this) {
                instance ?: AuthManager(context).also { instance = it }
            }
        }
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // GMS signing format: {iidToken}:{seconds}:{nanos}
    fun signIidToken(iidToken: String): Pair<ByteArray, Instant> {
        val now = System.currentTimeMillis()
        val timestamp = Instant.ofEpochMilli(now)
        val content = "$iidToken:${timestamp.epochSecond}:${timestamp.nano}"
        return sign(content) to timestamp
    }

    fun getIidToken(projectNumber: String? = null): String {
        return try {
            val sender = projectNumber ?: IidTokenPhenotypes.DEFAULT_PROJECT_NUMBER.toString()
            InstanceID.getInstance(context).getToken(sender, "GCM")
        } catch (_: Exception) {
            ""
        }
    }

    fun getOrCreateKeyPair(): KeyPair {
        val privateKeyStr = sharedPrefs.getString(KEY_PRIVATE, null)
        val publicKeyStr = sharedPrefs.getString(KEY_PUBLIC, null)

        if (privateKeyStr != null && publicKeyStr != null) {
            try {
                val kf = KeyFactory.getInstance("EC")
                val privateKey = kf.generatePrivate(
                    PKCS8EncodedKeySpec(Base64.decode(privateKeyStr, Base64.DEFAULT))
                )
                val publicKey = kf.generatePublic(
                    X509EncodedKeySpec(Base64.decode(publicKeyStr, Base64.DEFAULT))
                )
                return KeyPair(publicKey, privateKey)
            } catch (_: Exception) {
                // Fall through to regeneration on failure
            }
        }

        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        val kp = kpg.generateKeyPair()

        sharedPrefs.edit {
            putString(KEY_PRIVATE, Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
            putString(KEY_PUBLIC, Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP))
        }

        return kp
    }

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

    fun getFid(): String {
        return InstanceID.getInstance(context).id
    }
}
