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
import android.util.Log
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@TargetApi(21)
class NearbySharingScanner(
    private val context: Context,
    private val onDeviceFound: (NearbyDevice) -> Unit,
    private val onDeviceLost: (NearbyDevice) -> Unit
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val discoveredDevices = ConcurrentHashMap<String, NearbyDevice>()

    fun startDiscovery() {
        if (nsdManager == null) {
            Log.w(NearbySharingConstants.TAG, "NsdManager not available for discovery")
            return
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(NearbySharingConstants.TAG, "mDNS discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(NearbySharingConstants.TAG, "mDNS service found: ${service.serviceName}")
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(NearbySharingConstants.TAG, "mDNS service lost: ${service.serviceName}")
                val device = discoveredDevices.remove(service.serviceName)
                if (device != null) {
                    onDeviceLost(device)
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(NearbySharingConstants.TAG, "mDNS discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(NearbySharingConstants.TAG, "mDNS start discovery failed: $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(NearbySharingConstants.TAG, "mDNS stop discovery failed: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(
                NearbySharingConstants.SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(NearbySharingConstants.TAG, "Error starting service discovery", e)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                Log.w(NearbySharingConstants.TAG, "mDNS resolve failed: $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                val host = resolvedInfo.host
                val port = resolvedInfo.port
                val attributes = if (Build.VERSION.SDK_INT >= 21) {
                    resolvedInfo.attributes
                } else {
                    emptyMap()
                }
                val nAttr = attributes["n"]
                val endpointInfo = if (nAttr != null) {
                    NearbyEndpointInfo.parse(nAttr) ?: NearbyEndpointInfo.parseBase64(String(nAttr, Charsets.UTF_8))
                } else null

                val name = endpointInfo?.name ?: resolvedInfo.serviceName
                val type = endpointInfo?.deviceType ?: NearbyDeviceType.PHONE

                val device = NearbyDevice(
                    id = resolvedInfo.serviceName,
                    name = name,
                    type = type,
                    host = host,
                    port = port,
                    endpointInfo = nAttr
                )

                discoveredDevices[resolvedInfo.serviceName] = device
                onDeviceFound(device)
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(NearbySharingConstants.TAG, "Error resolving service", e)
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(NearbySharingConstants.TAG, "Error stopping service discovery", e)
            }
            discoveryListener = null
        }
        discoveredDevices.clear()
    }
}
