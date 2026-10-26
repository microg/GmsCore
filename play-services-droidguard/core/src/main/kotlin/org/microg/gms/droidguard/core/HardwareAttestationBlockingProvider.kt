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
        private var currentlyEnabled = false
        private lateinit var originalProvider: Provider
        private const val PROVIDER_NAME = "AndroidKeyStore"

        @JvmStatic
        fun ensureEnabled(enabled: Boolean = true) {
            if (currentlyEnabled == enabled) return
            try {
                if (enabled) {
                    Log.d(TAG, "Hardware attestation blocking enabled")
                    originalProvider = Security.getProvider(PROVIDER_NAME)
                    // Instantiate the SPI through the provider's own JCA service
                    // entry instead of reading KeyStore's private keyStoreSpi
                    // field — reflective field access fails with
                    // InaccessibleObjectException on strict-module runtimes.
                    val realSpi = originalProvider.getService("KeyStore", PROVIDER_NAME)
                        ?.newInstance(null) as? KeyStoreSpi
                        ?: throw KeyStoreException("$PROVIDER_NAME service missing on ${originalProvider.name}")

                    val newProvider = HardwareAttestationBlockingProvider(originalProvider, realSpi)
                    Security.removeProvider(PROVIDER_NAME)
                    Security.insertProviderAt(newProvider, 1)
                    currentlyEnabled = true
                } else {
                    Log.d(TAG, "Hardware attestation blocking disabled")
                    Security.removeProvider(PROVIDER_NAME)
                    Security.insertProviderAt(originalProvider, 1)
                    currentlyEnabled = false
                }
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
