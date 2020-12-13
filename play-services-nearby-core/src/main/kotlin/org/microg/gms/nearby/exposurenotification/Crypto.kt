/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.util.Log
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

private const val RPIK_HKDF_INFO = "EN-RPIK"
private const val RPIK_ALGORITHM = "AES"

private const val AEMK_HKDF_INFO = "EN-AEMK"
private const val AEMK_ALGORITHM = "AES"

private const val HKDF_ALGORITHM = "HmacSHA256"
private const val HKDF_LENGTH = 16
private const val HASH_LENGTH = 32

private const val RPID_ALGORITHM = "AES/ECB/NoPadding"
private const val RPID_PREFIX = "EN-RPI"
private const val AES_BLOCK_SIZE = 16

private const val AEM_ALGORITHM = "AES/CTR/NoPadding"

val currentIntervalNumber: Int
    get() = floor(System.currentTimeMillis() / 1000.0 / ROLLING_WINDOW_LENGTH).toInt()

val currentDayRollingStartNumber: Int
    get() = getDayRollingStartNumber(currentIntervalNumber)

fun getDayRollingStartNumber(intervalNumber: Int) = (floor(intervalNumber.toDouble() / ROLLING_PERIOD).toLong() * ROLLING_PERIOD).toInt()
fun getPeriodInDay(intervalNumber: Int) = intervalNumber - getDayRollingStartNumber(intervalNumber)

val nextKeyMillis: Long
    get() {
        val currentWindowStart = currentIntervalNumber.toLong() * ROLLING_WINDOW_LENGTH_MS
        val currentWindowEnd = currentWindowStart + ROLLING_WINDOW_LENGTH_MS
        return (currentWindowEnd - System.currentTimeMillis()).coerceAtLeast(0)
    }

fun generateTemporaryExposureKey(intervalNumber: Int): TemporaryExposureKey.TemporaryExposureKeyBuilder = TemporaryExposureKey.TemporaryExposureKeyBuilder().apply {
    var keyData = ByteArray(16)
    SecureRandom().nextBytes(keyData)
    setKeyData(keyData)
    setRollingStartIntervalNumber(intervalNumber)
    setRollingPeriod((ROLLING_PERIOD - getPeriodInDay(intervalNumber)))
}

fun generateCurrentDayTemporaryExposureKey(): TemporaryExposureKey = generateTemporaryExposureKey(currentDayRollingStartNumber).build()
fun generateIntraDayTemporaryExposureKey(intervalNumber: Int = currentIntervalNumber): TemporaryExposureKey = generateTemporaryExposureKey(intervalNumber).build()

@TargetApi(21)
fun TemporaryExposureKey.generateRpiKey(): SecretKeySpec {
    return SecretKeySpec(hkdf(keyData, null, RPIK_HKDF_INFO.toByteArray(StandardCharsets.UTF_8)), RPIK_ALGORITHM)
}

@TargetApi(21)
fun TemporaryExposureKey.generateAemKey(): SecretKeySpec {
    return SecretKeySpec(hkdf(keyData, null, AEMK_HKDF_INFO.toByteArray(StandardCharsets.UTF_8)), AEMK_ALGORITHM)
}

@TargetApi(21)
fun TemporaryExposureKey.generateRpiId(intervalNumber: Int): ByteArray {
    val cipher = Cipher.getInstance(RPID_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, generateRpiKey())
    val data = ByteBuffer.allocate(AES_BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
        put(RPID_PREFIX.toByteArray(StandardCharsets.UTF_8))
        position(12)
        putInt(intervalNumber)
    }.array()
    return cipher.doFinal(data)
}

@TargetApi(21)
fun TemporaryExposureKey.generateAllRpiIds(): ByteArray {
    val cipher = Cipher.getInstance(RPID_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, generateRpiKey())
    val data = ByteBuffer.allocate(AES_BLOCK_SIZE * rollingPeriod).order(ByteOrder.LITTLE_ENDIAN).apply {
        val prefix = RPID_PREFIX.toByteArray(StandardCharsets.UTF_8)
        for (i in 0 until rollingPeriod) {
            put(prefix)
            position(i * 16 + 12)
            putInt(rollingStartIntervalNumber + i)
        }
    }.array()
    return cipher.doFinal(data)
}

fun TemporaryExposureKey.cryptAem(rpi: ByteArray, metadata: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(AEM_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, generateAemKey(), IvParameterSpec(rpi))
    return cipher.doFinal(metadata)
}

fun TemporaryExposureKey.generatePayload(intervalNumber: Int, metadata: ByteArray): ByteArray {
    val rpi = generateRpiId(intervalNumber)
    val aem = cryptAem(rpi, metadata)
    return rpi + aem
}

private fun hkdf(inputKeyingMaterial: ByteArray, inputSalt: ByteArray?, info: ByteArray): ByteArray {
    val mac = Mac.getInstance(HKDF_ALGORITHM)
    val salt = if (inputSalt == null || inputSalt.isEmpty()) ByteArray(HASH_LENGTH) else inputSalt
    mac.init(SecretKeySpec(salt, HKDF_ALGORITHM))
    val pseudoRandomKey = mac.doFinal(inputKeyingMaterial)
    mac.init(SecretKeySpec(pseudoRandomKey, HKDF_ALGORITHM))
    mac.update(info)
    return Arrays.copyOf(mac.doFinal(byteArrayOf(0x01)), HKDF_LENGTH)
}
