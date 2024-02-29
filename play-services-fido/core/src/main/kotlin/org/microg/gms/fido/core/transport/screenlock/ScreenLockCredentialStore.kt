/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.screenlock

import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build.VERSION.SDK_INT
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.fido.core.UserInfo
import org.microg.gms.utils.toBase64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.ProviderException
import java.security.PublicKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.spec.ECGenParameterSpec
import kotlin.random.Random

@RequiresApi(23)
class ScreenLockCredentialStore(val context: Context) : SQLiteOpenHelper(context, "screenlockcredentials.db", null, DATABASE_VERSION) {
    private val keyStore by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }

    private fun getAlias(rpId: String, keyId: ByteArray): String =
        "1." + keyId.toBase64(Base64.NO_PADDING, Base64.NO_WRAP) + "." + rpId

    private fun getPrivateKey(rpId: String, keyId: ByteArray) = keyStore.getKey(getAlias(rpId, keyId), null) as? PrivateKey

    @RequiresApi(23)
    fun createKey(rpId: String, challenge: ByteArray): ByteArray {
        clearInvalidatedKeys()

        var useStrongbox = false
        if (SDK_INT >= 28) useStrongbox = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        val keyId = Random.nextBytes(32)
        val identifier = getAlias(rpId, keyId)
        Log.d(TAG, "Creating key for $identifier")
        val generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(identifier, KeyProperties.PURPOSE_SIGN)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setUserAuthenticationRequired(true)
        if (SDK_INT >= 28) builder.setIsStrongBoxBacked(useStrongbox)
        if (SDK_INT >= 24) builder.setAttestationChallenge(challenge)

        var generatedKeypair = false
        val exceptionClassesCaught = HashSet<Class<Exception>>()
        while (!generatedKeypair) {
            try {
                generator.initialize(builder.build())
                generator.generateKeyPair()
                generatedKeypair = true
            } catch (e: Exception) {
                // Catch each exception class at most once.
                // If we've caught the exception before, tried to correct it, and still catch the
                // same exception, then we can't fix it and the exception should be thrown further
                if (exceptionClassesCaught.contains(e.javaClass)) {
                    throw e
                }
                exceptionClassesCaught.add(e.javaClass)

                if (SDK_INT >= 28 && e is StrongBoxUnavailableException) {
                    Log.w(TAG, "Failed with StrongBox, retrying without it...")
                    // Not all algorithms are backed by the Strongbox. If the Strongbox doesn't
                    // support this keypair, fall back to TEE
                    builder.setIsStrongBoxBacked(false)
                } else if (SDK_INT >= 24 && e is ProviderException) {
                    Log.w(TAG, "Failed with attestation challenge, retrying without it...")
                    // This ProviderException is often thrown if the TEE or Strongbox doesn't have
                    // a built-in key to attest the new key pair with. If this happens, remove the
                    // attestation challenge and create an unattested key
                    builder.setAttestationChallenge(null)
                } else {
                    // We don't know how to handle other errors, so they should be thrown up the
                    // system
                    throw e
                }
            }
        }
        return keyId
    }

    fun getPublicKey(rpId: String, keyId: ByteArray): PublicKey? {
        clearInvalidatedKeys()
        return keyStore.getCertificate(getAlias(rpId, keyId))?.publicKey
    }

    fun getPublicKeys(rpId: String): Collection<Pair<String, PublicKey>> {
        clearInvalidatedKeys()

        val keys = ArrayList<Pair<String, PublicKey>>()
        for (alias in keyStore.aliases()) {
            if (alias.endsWith(".$rpId")) {
                val key = keyStore.getCertificate(alias).publicKey
                keys.add(Pair(alias, key))
            }
        }

        return keys
    }

    fun deleteKey(rpId:String, keyId: ByteArray) {
        val keyAlias = getAlias(rpId, keyId)
        Log.w(TAG, "Deleting key with alias $keyAlias")
        keyStore.deleteEntry(keyAlias)
    }

    fun clearInvalidatedKeys() {
        // Iterate through the keys, try to initiate them, and delete them if this throws an
        // invalidated exception
        val keysToDelete = ArrayList<String>()
        for (alias in keyStore.aliases()) {
            try {
                // This is a bit of a hack, but if you try to initSign on a key that has been
                // invalidated, it throws a KeyPermanentlyInvalidatedException if the key is
                // invalidated. Otherwise, it throws an exception that I assume is related to
                // the lack of biometric authentication
                val key = keyStore.getKey(alias, null) as? PrivateKey
                val signature = Signature.getInstance("SHA256withECDSA")
                signature.initSign(key)
            } catch (e: KeyPermanentlyInvalidatedException) {
                Log.w(TAG, "Removing permanently invalidated key with alias $alias")
                keysToDelete.add(alias)
            } catch (e: Exception) {
                // Any other exception, we just continue
            }
        }

        for (alias in keysToDelete) {
            keyStore.deleteEntry(alias)
        }
    }


    fun getCertificateChain(rpId: String, keyId: ByteArray): Array<Certificate> =
        keyStore.getCertificateChain(getAlias(rpId, keyId))

    fun getSignature(rpId: String, keyId: ByteArray): Signature? {
        try {
            val privateKey = getPrivateKey(rpId, keyId) ?: return null
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            return signature
        } catch (e: KeyPermanentlyInvalidatedException) {
            keyStore.deleteEntry(getAlias(rpId, keyId))
            throw e
        }
    }

    fun containsKey(rpId: String, keyId: ByteArray): Boolean = keyStore.containsAlias(getAlias(rpId, keyId))

    companion object {
        const val TAG = "FidoLockStore"

        const val DATABASE_VERSION = 1

        const val TABLE_DISPLAY_NAMES = "DISPLAY_NAMES_TABLE"
        const val COLUMN_KEY_ALIAS = "KEY_ALIAS_COLUMN"
        const val COLUMN_NAME = "NAME_COLUMN"
        const val COLUMN_DISPLAY_NAME = "DISPLAY_NAME_COLUMN"
        const val COLUMN_ICON = "ICON_COLUMN"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        onUpgrade(db, 0, DATABASE_VERSION)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db == null) {
            return
        }
        if (oldVersion < 1) {
            db.execSQL("CREATE TABLE $TABLE_DISPLAY_NAMES($COLUMN_KEY_ALIAS TEXT NOT NULL, $COLUMN_NAME TEXT NOT NULL, $COLUMN_DISPLAY_NAME TEXT, $COLUMN_ICON TEXT, UNIQUE($COLUMN_KEY_ALIAS) ON CONFLICT REPLACE)")
        }
    }

    fun addUserInfo(rpId: String, keyId: ByteArray, userInfo: UserInfo) {
        addUserInfo(rpId, keyId, userInfo.name, userInfo.displayName, userInfo.icon)
    }

    fun addUserInfo(rpId: String, keyId: ByteArray, name: String, displayName: String? = null, icon: String? = null) = writableDatabase.use {
        // Since this function is not called very often, calling cleanDatabase here will probably not
        // slow things down by much, and it will avoid the database growing larger than necessary
        cleanDatabase(it)

        // The key alias and display names are both coming from outside sources. Don't trust them
        val keyAlias = getAlias(rpId, keyId)
        val insertStatement = it.compileStatement("INSERT INTO $TABLE_DISPLAY_NAMES($COLUMN_KEY_ALIAS, $COLUMN_NAME, $COLUMN_DISPLAY_NAME, $COLUMN_ICON) VALUES(?, ?, ?, ?)")
        insertStatement.bindString(1, keyAlias)
        insertStatement.bindString(2, name)
        if (displayName != null) insertStatement.bindString(3, displayName)
        if (icon != null) insertStatement.bindString(4, icon)
        insertStatement.executeInsert()
    }

    fun getUserInfo(rpId: String, keyId: ByteArray): UserInfo? = writableDatabase.use {
        // Same argument as above, this function is not called often, so cleaning every time it's called
        // should not slow down the phone a lot
        cleanDatabase(it)

        val keyAlias = getAlias(rpId, keyId)
        val userInfoQuery = it.query(TABLE_DISPLAY_NAMES, arrayOf(COLUMN_NAME, COLUMN_DISPLAY_NAME, COLUMN_ICON), "$COLUMN_KEY_ALIAS = ?", arrayOf(keyAlias), null, null, null, null)

        var name: String? = null
        var displayName: String? = null
        var icon: String? = null
        userInfoQuery.use { cursor ->
            if (cursor.moveToNext()) {
                name = cursor.getString(0)
                displayName = cursor.getString(1)
                icon = cursor.getString(2)
            }
        }

        if (name != null) {
            return UserInfo(name!!, displayName, icon)
        } else {
            return null
        }
    }

    private fun cleanDatabase(db: SQLiteDatabase) {
        // Remove all display names that don't have an alias in the keystore
        val aliases = HashSet<String>()
        for (alias in keyStore.aliases()) {
            aliases.add(alias)
        }

        val aliasesToDelete = HashSet<String>()
        val knownAliases = db.query(TABLE_DISPLAY_NAMES, arrayOf(COLUMN_KEY_ALIAS), null, null, null, null, null)
        knownAliases.use { cursor ->
            while (cursor.moveToNext()) {
                val databaseAlias = cursor.getString(0)
                if (!aliases.contains(databaseAlias)) aliasesToDelete.add(databaseAlias)
            }
        }

        // Since key IDs come from outside microG, treat them as potentially suspicious
        // Use prepared statements to avoid SQL injections
        val preparedDeleteStatement = db.compileStatement("DELETE FROM $TABLE_DISPLAY_NAMES WHERE $COLUMN_KEY_ALIAS = ?")
        for (aliasToDelete in aliasesToDelete) {
            Log.w(TAG, "Removing userinfo for key with alias $aliasToDelete, since key no longer exists")
            preparedDeleteStatement.bindString(1, aliasToDelete)
            preparedDeleteStatement.executeUpdateDelete()
        }
    }
}
