/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: This is heavily inspired by "Universal SafetyNet Fix", used under the terms of MIT License,
 *         Copyright (c) 2021 Danny Lin <danny@kdrag0n.dev>
 */

package org.microg.gms.droidguard.core

import android.util.Log
import androidx.annotation.Keep
import java.io.InputStream
import java.io.OutputStream
import java.security.*
import java.security.cert.Certificate
import java.util.*

private const val TAG = "DroidGuard"

class HardwareAttestationBlockingProvider(
    realProvider: Provider,
    realSpi: KeyStoreSpi
) : Provider(realProvider.name, realProvider.version, realProvider.info) {
    init {
        HardwareAttestationBlockingKeyStore.realSpi = realSpi
        this["KeyStore.$PROVIDER_NAME"] = HardwareAttestationBlockingKeyStore::class.java.name
    }

    companion object {
        private var initialized = false
        private const val PROVIDER_NAME = "AndroidKeyStore"
        private const val FIELD_KEY_STORE_SPI = "keyStoreSpi"

        @JvmStatic
        fun ensureInitialized() {
            if (initialized) return
            try {
                val realProvider = Security.getProvider(PROVIDER_NAME)
                val realKeystore = KeyStore.getInstance(PROVIDER_NAME)
                val realSpi = realKeystore.get<KeyStoreSpi>(FIELD_KEY_STORE_SPI)

                val newProvider = HardwareAttestationBlockingProvider(realProvider, realSpi)
                Security.removeProvider(PROVIDER_NAME)
                Security.insertProviderAt(newProvider, 1)
                initialized = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed replacing the security provider", e)
            }
        }
    }
}

class HardwareAttestationBlockingKeyStore(private val realSpi: KeyStoreSpi) : KeyStoreSpi() {
    @Keep
    constructor() : this(Companion.realSpi ?: throw IllegalStateException())

    override fun engineGetCertificateChain(alias: String?): Array<Certificate>? {
        for (stackTraceElement in Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.className.lowercase().contains("droidguard")) {
                Log.d(TAG, "Block DroidGuard from accessing engineGetCertificateChain")
                throw UnsupportedOperationException()
            }
        }
        return realSpi.engineGetCertificateChain(alias)
    }

    override fun engineGetKey(alias: String?, password: CharArray?): Key? = realSpi.engineGetKey(alias, password)
    override fun engineGetCertificate(alias: String?): Certificate? = realSpi.engineGetCertificate(alias)
    override fun engineGetCreationDate(alias: String?): Date? = realSpi.engineGetCreationDate(alias)
    override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<out Certificate>?) = realSpi.engineSetKeyEntry(alias, key, password, chain)
    override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) = realSpi.engineSetKeyEntry(alias, key, chain)
    override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) = realSpi.engineSetCertificateEntry(alias, cert)
    override fun engineDeleteEntry(alias: String?) = realSpi.engineDeleteEntry(alias)
    override fun engineAliases(): Enumeration<String>? = realSpi.engineAliases()
    override fun engineContainsAlias(alias: String?) = realSpi.engineContainsAlias(alias)
    override fun engineSize() = realSpi.engineSize()
    override fun engineIsKeyEntry(alias: String?) = realSpi.engineIsKeyEntry(alias)
    override fun engineIsCertificateEntry(alias: String?) = realSpi.engineIsCertificateEntry(alias)
    override fun engineGetCertificateAlias(cert: Certificate?): String? = realSpi.engineGetCertificateAlias(cert)
    override fun engineStore(stream: OutputStream?, password: CharArray?) = realSpi.engineStore(stream, password)
    override fun engineLoad(stream: InputStream?, password: CharArray?) = realSpi.engineLoad(stream, password)

    companion object {
        var realSpi: KeyStoreSpi? = null
    }
}

private fun <T> Any.get(name: String) = this::class.java.getDeclaredField(name).let { field ->
    field.isAccessible = true
    @Suppress("unchecked_cast")
    field.get(this) as T
}
