package org.microg.gms.constellation.verification.ts43

import android.os.Build
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val TAG = "EapAkaService"

class EapAkaService(private val telephonyManager: TelephonyManager) {

    companion object {
        private const val EAP_CODE_REQUEST = 1
        private const val EAP_CODE_RESPONSE = 2

        private const val EAP_TYPE_AKA = 23

        private const val EAP_AKA_SUBTYPE_CHALLENGE = 1
        private const val EAP_AKA_SUBTYPE_SYNC_FAILURE = 4

        private const val AT_RAND = 1
        private const val AT_AUTN = 2
        private const val AT_RES = 3
        private const val AT_AUTS = 4
        private const val AT_MAC = 11

        private const val SIM_RES_SUCCESS = 0xDB.toByte()
        private const val SIM_RES_SYNC_FAIL = 0xDC.toByte()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun performSimAkaAuth(eapRelayBase64: String, imsi: String, mccMnc: String): String? {
        val eapPacket = Base64.decode(eapRelayBase64, Base64.DEFAULT)
        if (eapPacket.size < 12) return null

        val code = eapPacket[0].toInt()
        val eapId = eapPacket[1]
        val type = eapPacket[4].toInt()
        val subtype = eapPacket[5].toInt()

        if (code != EAP_CODE_REQUEST || type != EAP_TYPE_AKA || subtype != EAP_AKA_SUBTYPE_CHALLENGE) {
            Log.w(TAG, "Unexpected EAP packet: code=$code, type=$type, subtype=$subtype")
            return null
        }

        // Parse attributes (starting at offset 8)
        var rand: ByteArray? = null
        var autn: ByteArray? = null

        var offset = 8
        while (offset + 2 <= eapPacket.size) {
            val attrType = eapPacket[offset].toInt() and 0xFF
            val attrLen = (eapPacket[offset + 1].toInt() and 0xFF) * 4
            if (offset + attrLen > eapPacket.size || attrLen < 4) break

            when (attrType) {
                AT_RAND -> {
                    if (attrLen >= 20) {
                        rand = ByteArray(16)
                        System.arraycopy(eapPacket, offset + 4, rand, 0, 16)
                    }
                }

                AT_AUTN -> {
                    if (attrLen >= 20) {
                        autn = ByteArray(16)
                        System.arraycopy(eapPacket, offset + 4, autn, 0, 16)
                    }
                }
            }
            offset += attrLen
            if (rand != null && autn != null) break
        }

        if (rand == null || autn == null) {
            Log.e(TAG, "Missing RAND or AUTN in EAP-AKA challenge")
            return null
        }

        val challengeBytes = byteArrayOf(16) + rand + byteArrayOf(16) + autn
        val challengeB64 = Base64.encodeToString(challengeBytes, Base64.NO_WRAP)

        val iccAuthResult = telephonyManager.getIccAuthentication(
            TelephonyManager.APPTYPE_USIM, TelephonyManager.AUTHTYPE_EAP_AKA, challengeB64
        ) ?: run {
            Log.e(TAG, "SIM returned null for AKA auth")
            return null
        }

        val iccBytes = Base64.decode(iccAuthResult, Base64.DEFAULT)
        if (iccBytes.isEmpty()) return null

        return when (iccBytes[0]) {
            SIM_RES_SUCCESS -> {
                val res = extractTlv(1, iccBytes) ?: return null
                val ck = extractTlv(1 + res.size + 1, iccBytes) ?: return null
                val ik = extractTlv(1 + res.size + 1 + ck.size + 1, iccBytes) ?: return null

                val identity = buildEapId(mccMnc, imsi)
                val identityBytes = identity.toByteArray(StandardCharsets.UTF_8)
                val keys = Fips186Prf.deriveKeys(identityBytes, ik, ck)

                val kAut = keys["K_aut"] ?: run {
                    Log.e(TAG, "Failed to derive K_aut")
                    return null
                }

                val responsePacket = buildEapAkaResponse(eapId, res, kAut) ?: return null
                Base64.encodeToString(responsePacket, Base64.NO_WRAP)
            }

            SIM_RES_SYNC_FAIL -> {
                val auts = extractTlv(1, iccBytes) ?: return null
                val responsePacket = buildEapAkaSyncFailure(eapId, auts)
                Base64.encodeToString(responsePacket, Base64.NO_WRAP)
            }

            else -> {
                Log.e(TAG, "Unknown SIM response tag: ${iccBytes[0]}")
                null
            }
        }
    }

    fun buildEapId(mccMnc: String, imsi: String, realm: String? = null): String {
        val mcc = mccMnc.substring(0, 3)
        var mnc = mccMnc.substring(3)
        if (mnc.length == 2) mnc = "0$mnc" // Zero-pad 2-digit MNCs
        val defaultRealm = "nai.epc.mnc$mnc.mcc$mcc.3gppnetwork.org"
        val resolvedRealm = when {
            realm.isNullOrBlank() -> defaultRealm
            realm == "nai.epc" -> defaultRealm
            realm.contains(".mnc") && realm.contains(".mcc") && realm.contains("3gppnetwork.org") -> realm
            else -> realm
        }
        return "0$imsi@$resolvedRealm"
    }

    private fun extractTlv(index: Int, data: ByteArray): ByteArray? {
        if (index >= data.size) return null
        val len = data[index].toInt() and 0xFF
        if (index + 1 + len > data.size) return null
        return data.copyOfRange(index + 1, index + 1 + len)
    }

    private fun buildEapAkaResponse(id: Byte, res: ByteArray, kAut: ByteArray): ByteArray? {
        val resAttrLen = ((res.size + 4 + 3) / 4) * 4
        val totalLen = 8 + resAttrLen + 20
        val buffer = ByteBuffer.allocate(totalLen)

        buffer.put(EAP_CODE_RESPONSE.toByte())
        buffer.put(id)
        buffer.putShort(totalLen.toShort())
        buffer.put(EAP_TYPE_AKA.toByte())
        buffer.put(EAP_AKA_SUBTYPE_CHALLENGE.toByte())
        buffer.putShort(0)

        buffer.put(AT_RES.toByte())
        buffer.put((resAttrLen / 4).toByte())
        buffer.putShort((res.size * 8).toShort())
        buffer.put(res)

        val padding = resAttrLen - 4 - res.size
        if (padding > 0) buffer.put(ByteArray(padding))

        buffer.position()
        buffer.put(AT_MAC.toByte())
        buffer.put(5)
        buffer.putShort(0)
        val macValueOffset = buffer.position()
        buffer.put(ByteArray(16))

        val packet = buffer.array()
        val mac = hmacSha1(kAut, packet) ?: return null

        System.arraycopy(mac, 0, packet, macValueOffset, 16)
        return packet
    }

    private fun buildEapAkaSyncFailure(id: Byte, auts: ByteArray): ByteArray {
        val attrLen = ((auts.size + 2 + 3) / 4) * 4
        val totalLen = 8 + attrLen

        return ByteBuffer.allocate(totalLen).apply {
            put(EAP_CODE_RESPONSE.toByte())
            put(id)
            putShort(totalLen.toShort())
            put(EAP_TYPE_AKA.toByte())
            put(EAP_AKA_SUBTYPE_SYNC_FAILURE.toByte())
            putShort(0)

            put(AT_AUTS.toByte())
            put((attrLen / 4).toByte())
            put(auts)

            val padding = attrLen - 2 - auts.size
            if (padding > 0) put(ByteArray(padding))
        }.array()
    }

    private fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray? = try {
        Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(key, "HmacSHA1")) }.doFinal(data)
    } catch (_: Exception) {
        null
    }
}
