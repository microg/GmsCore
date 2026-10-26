/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.util.Base64
import com.google.android.gms.phenotype.Flag
import com.google.common.hash.Hashing
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

internal object FlagsContentCodec {

    private const val MAX_NAME_HASH = 0x1FFFFFFFFFFFFFFFL
    private val flagOrder = Comparator<PhixitFlag> { left, right ->
        val hashComparison = left.nameHash.compareTo(right.nameHash)
        if (hashComparison != 0 || left.nameHash != 0L) hashComparison
        else left.name.orEmpty().compareTo(right.name.orEmpty())
    }

    const val PHIXIT_BOOL_FALSE = 0
    const val PHIXIT_BOOL_TRUE = 1
    const val PHIXIT_INT = 2
    const val PHIXIT_DOUBLE = 3
    const val PHIXIT_STRING = 4
    const val PHIXIT_BYTES = 5

    data class PhixitFlag(
        val nameHash: Long,
        val name: String?,
        val type: Int,
        val longValue: Long = 0,
        val stringValue: String? = null,
        val bytesValue: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PhixitFlag) return false
            return nameHash == other.nameHash && name == other.name && type == other.type
                    && longValue == other.longValue && stringValue == other.stringValue
                    && bytesValue.contentEquals(other.bytesValue)
        }

        override fun hashCode(): Int {
            var result = nameHash.hashCode()
            result = 31 * result + (name?.hashCode() ?: 0)
            result = 31 * result + type
            return result
        }
    }

    fun nameToHash(name: String): Long {
        if (name.isEmpty() || name.length > 19) return 0
        if (name[0] == '0' && name.length > 1) return 0
        for (c in name) {
            if (c !in '0'..'9') return 0
        }
        val value = name.toLongOrNull() ?: return 0
        return if (value in 1..MAX_NAME_HASH) value else 0
    }

    fun encode(flags: List<PhixitFlag>): ByteArray {
        val sorted = flags.sortedWith(flagOrder)
        require(sorted.zipWithNext().none { (left, right) -> flagOrder.compare(left, right) == 0 }) {
            "Duplicate Phixit flag name"
        }
        val raw = encodeRaw(sorted)
        return compress(raw)
    }

    fun encodeSharedFile(metadata: SharedConfigurationMetadata, flags: List<PhixitFlag>): ByteArray {
        val metadataBytes = metadata.encode()
        val flagsBytes = encode(flags)
        return ByteArrayOutputStream().apply {
            writeVarint(this, 1)
            writeVarint(this, 0)
            writeVarint(this, metadataBytes.size.toLong())
            write(metadataBytes)
            writeVarint(this, flagsBytes.size.toLong())
            write(flagsBytes)
        }.toByteArray()
    }

    private fun encodeRaw(sortedFlags: List<PhixitFlag>): ByteArray {
        val out = ByteArrayOutputStream()
        writeVarint(out, sortedFlags.size.toLong())

        var prevHash = 0L
        for (flag in sortedFlags) {
            val delta = flag.nameHash - prevHash
            val encoded = (delta shl 3) or flag.type.toLong()
            writeVarint(out, encoded)
            prevHash = flag.nameHash

            if (flag.nameHash == 0L && flag.name != null) {
                val nameBytes = flag.name.toByteArray(Charsets.UTF_8)
                writeVarint(out, nameBytes.size.toLong())
                out.write(nameBytes)
            }

            when (flag.type) {
                PHIXIT_BOOL_FALSE, PHIXIT_BOOL_TRUE -> { /* no value data */
                }

                PHIXIT_INT -> writeVarint(out, flag.longValue)
                PHIXIT_DOUBLE -> {
                    val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                    buf.putLong(flag.longValue)
                    out.write(buf.array())
                }

                PHIXIT_STRING -> {
                    val strBytes = (flag.stringValue ?: "").toByteArray(Charsets.UTF_8)
                    writeVarint(out, strBytes.size.toLong())
                    out.write(strBytes)
                }

                PHIXIT_BYTES -> {
                    val bytes = flag.bytesValue ?: byteArrayOf()
                    writeVarint(out, bytes.size.toLong())
                    out.write(bytes)
                }

                else -> throw IllegalArgumentException("Unknown Phixit flag type: ${flag.type}")
            }
        }
        return out.toByteArray()
    }

    fun sharedFileRelativePath(secret: ByteArray, packageName: String, user: String): String {
        val secretDirectory = Base64.encodeToString(secret, Base64.URL_SAFE or Base64.NO_WRAP)
        val identity = packageName.toByteArray(Charsets.UTF_8) + byteArrayOf(0) + user.toByteArray(Charsets.UTF_8)
        val fileName = Base64.encodeToString(
            Hashing.murmur3_128(0).hashBytes(identity).asBytes(),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "$secretDirectory/$fileName.pb"
    }

    fun fromLegacyFlags(flags: List<Flag>): List<PhixitFlag> = flags.map { flag ->
        val name = flag.name ?: ""
        val hash = nameToHash(name)
        val nameStr = if (hash == 0L) name else null

        when (flag.dataType) {
            Flag.DATA_TYPE_LONG -> PhixitFlag(hash, nameStr, PHIXIT_INT, longValue = flag.getLong())
            Flag.DATA_TYPE_BOOL -> {
                if (flag.getBool()) PhixitFlag(hash, nameStr, PHIXIT_BOOL_TRUE)
                else PhixitFlag(hash, nameStr, PHIXIT_BOOL_FALSE)
            }

            Flag.DATA_TYPE_DOUBLE -> {
                val bits = java.lang.Double.doubleToRawLongBits(flag.getDouble())
                PhixitFlag(hash, nameStr, PHIXIT_DOUBLE, longValue = bits)
            }

            Flag.DATA_TYPE_STRING -> PhixitFlag(hash, nameStr, PHIXIT_STRING, stringValue = flag.getString())
            Flag.DATA_TYPE_BYTES -> PhixitFlag(hash, nameStr, PHIXIT_BYTES, bytesValue = flag.getBytes())
            else -> throw IllegalArgumentException("Unrecognized flag type: ${flag.dataType}")
        }
    }

    private fun compress(data: ByteArray): ByteArray {
        val deflater = Deflater(1, true)
        try {
            val out = ByteArrayOutputStream()
            DeflaterOutputStream(out, deflater).use { it.write(data) }
            return out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (true) {
            if ((v and 0x7FL.inv()) == 0L) {
                out.write(v.toInt())
                return
            }
            out.write((v.toInt() and 0x7F) or 0x80)
            v = v ushr 7
        }
    }
}
