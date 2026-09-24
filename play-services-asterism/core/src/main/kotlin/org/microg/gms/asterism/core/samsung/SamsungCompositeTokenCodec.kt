package org.microg.gms.asterism.core.samsung

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

internal object SamsungCompositeTokenCodec {
    fun encodeProto(iidToken: String, piaToken: String): ByteArray {
        require(iidToken.isNotBlank()) { "IID token is required" }
        require(piaToken.isNotBlank()) { "PIA token is required" }
        return ByteArrayOutputStream().also { out ->
            writeStringField(out, 1, iidToken)
            writeStringField(out, 2, piaToken)
        }.toByteArray()
    }
    fun encodeForIms(iidToken: String, piaToken: String): String =
        Base64.encodeToString(encodeProto(iidToken, piaToken), Base64.NO_WRAP or Base64.URL_SAFE)
    fun encodeForImsOrNull(iidToken: String?, piaToken: String?): String? {
        if (iidToken.isNullOrBlank() || piaToken.isNullOrBlank()) return null
        return encodeForIms(iidToken, piaToken)
    }
    private fun writeStringField(out: ByteArrayOutputStream, field: Int, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeVarint(out, (field shl 3) or 2)
        writeVarint(out, bytes.size)
        out.write(bytes)
    }
    private fun writeVarint(out: ByteArrayOutputStream, input: Int) {
        var value = input
        while (true) {
            if (value and 0x7f.inv() == 0) { out.write(value); return }
            out.write((value and 0x7f) or 0x80)
            value = value ushr 7
        }
    }
}
