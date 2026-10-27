/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing
import android.annotation.TargetApi
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import okio.ByteString.Companion.toByteString
import android.util.Log
import java.security.SecureRandom

@TargetApi(21)
class NearbySharingAdvertiser(
    private val context: Context,
    private val deviceName: String,
    private val deviceType: NearbyDeviceType = NearbyDeviceType.PHONE
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    val endpointId: String = generateEndpointId()

    private fun generateEndpointId(): String {
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = SecureRandom()
        val sb = StringBuilder(4)
        for (i in 0 until 4) {
            sb.append(alphabet[random.nextInt(alphabet.length)])
        }
        return sb.toString()
    }

    private fun buildServiceName(): String {
        val bytes = ByteArray(10)
        bytes[0] = NearbySharingConstants.PCP_BYTE
        val epBytes = endpointId.toByteArray(Charsets.US_ASCII)
        System.arraycopy(epBytes, 0, bytes, 1, Math.min(epBytes.size, 4))
        System.arraycopy(NearbySharingConstants.SERVICE_ID_HASH, 0, bytes, 5, 3)
        bytes[8] = 0
        bytes[9] = 0
        return bytes.toByteString().base64Url().replace("=", "")
    }

    fun startAdvertising(port: Int, onStarted: (() -> Unit)? = null, onError: ((Int) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < 21) {
            Log.w(NearbySharingConstants.TAG, "Nearby Sharing requires API 21+")
            onError?.invoke(-1)
            return
        }
        if (nsdManager == null) {
            Log.w(NearbySharingConstants.TAG, "NsdManager not available")
            onError?.invoke(-1)
            return
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceType = NearbySharingConstants.SERVICE_TYPE
            serviceName = buildServiceName()
            setPort(port)
            val info = NearbyEndpointInfo(deviceName, deviceType)
            setAttribute("n", info.toBase64())
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredInfo: NsdServiceInfo) {
                Log.d(NearbySharingConstants.TAG, "mDNS service registered: ${registeredInfo.serviceName} on port $port")
                onStarted?.invoke()
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(NearbySharingConstants.TAG, "mDNS registration failed: $errorCode")
                onError?.invoke(errorCode)
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(NearbySharingConstants.TAG, "mDNS service unregistered")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(NearbySharingConstants.TAG, "mDNS unregistration failed: $errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(NearbySharingConstants.TAG, "Exception during registerService", e)
            onError?.invoke(-1)
        }
    }

    fun stopAdvertising() {
        registrationListener?.let {
            try {
                nsdManager?.unregisterService(it)
            } catch (e: Exception) {
                Log.e(NearbySharingConstants.TAG, "Error unregistering service", e)
            }
            registrationListener = null
        }
    }
}
