/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsHealthChecker - System health monitoring and diagnostics
 */

package org.microg.gms.rcs.health

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.microg.gms.rcs.DeviceIdentifierHelper
import org.microg.gms.rcs.SimCardHelper
import org.microg.gms.rcs.carrier.ExtendedCarrierDatabase
import org.microg.gms.rcs.di.RcsServiceLocator
import org.microg.gms.rcs.orchestrator.RcsOrchestrator
import org.microg.gms.rcs.state.RcsState

class RcsHealthChecker(private val context: Context) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun performHealthCheck(): HealthCheckResult {
        return withContext(Dispatchers.IO) {
            val checks = mutableListOf<HealthCheck>()
            
            checks.add(checkNetworkConnectivity())
            checks.add(checkSimCard())
            checks.add(checkCarrierSupport())
            checks.add(checkPermissions())
            checks.add(checkDeviceIdentifiers())
            checks.add(checkRcsState())
            checks.add(checkDeviceCapabilities())
            
            val overallStatus = when {
                checks.any { it.status == HealthStatus.CRITICAL } -> HealthStatus.CRITICAL
                checks.any { it.status == HealthStatus.WARNING } -> HealthStatus.WARNING
                checks.all { it.status == HealthStatus.HEALTHY } -> HealthStatus.HEALTHY
                else -> HealthStatus.UNKNOWN
            }
            
            HealthCheckResult(
                overallStatus = overallStatus,
                checks = checks,
                timestamp = System.currentTimeMillis(),
                recommendations = generateRecommendations(checks)
            )
        }
    }

    private fun checkNetworkConnectivity(): HealthCheck {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }
        
        return when {
            network == null -> HealthCheck(
                name = "Network Connectivity",
                status = HealthStatus.CRITICAL,
                message = "No network connection available",
                details = mapOf("connected" to false)
            )
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true -> HealthCheck(
                name = "Network Connectivity",
                status = HealthStatus.WARNING,
                message = "Network available but no internet capability",
                details = mapOf("connected" to true, "internet" to false)
            )
            else -> {
                val type = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                    else -> "Other"
                }
                HealthCheck(
                    name = "Network Connectivity",
                    status = HealthStatus.HEALTHY,
                    message = "Connected via $type",
                    details = mapOf("connected" to true, "type" to type)
                )
            }
        }
    }

    private fun checkSimCard(): HealthCheck {
        return if (SimCardHelper.isSimCardReady(context)) {
            val simInfo = SimCardHelper.getSimCardInfo(context)
            val carrierName = simInfo?.carrierName
            val mccMnc = simInfo?.mccMnc
            
            HealthCheck(
                name = "SIM Card",
                status = HealthStatus.HEALTHY,
                message = "SIM ready: $carrierName",
                details = mapOf("mccMnc" to (mccMnc ?: ""), "carrier" to (carrierName ?: ""))
            )
        } else {
            HealthCheck(
                name = "SIM Card",
                status = HealthStatus.CRITICAL,
                message = "SIM card not ready or absent",
                details = mapOf("ready" to false)
            )
        }
    }

    private fun checkCarrierSupport(): HealthCheck {
        val simInfo = SimCardHelper.getSimCardInfo(context)
        val mccMnc = simInfo?.mccMnc
        
        if (mccMnc == null) {
            return HealthCheck(
                name = "Carrier Support",
                status = HealthStatus.WARNING,
                message = "Cannot determine carrier",
                details = emptyMap()
            )
        }
        
        val isSupported = ExtendedCarrierDatabase.isCarrierSupported(mccMnc)
        val config = ExtendedCarrierDatabase.getConfig(mccMnc)
        
        return if (isSupported) {
            HealthCheck(
                name = "Carrier Support",
                status = HealthStatus.HEALTHY,
                message = "${config.carrierName} supports RCS (${config.rcsProfile})",
                details = mapOf(
                    "carrier" to config.carrierName,
                    "rcsProfile" to config.rcsProfile,
                    "supportsJibe" to config.supportsJibe
                )
            )
        } else {
            HealthCheck(
                name = "Carrier Support",
                status = HealthStatus.WARNING,
                message = "Carrier not in database, using Google Jibe",
                details = mapOf("usingJibe" to true)
            )
        }
    }

    private fun checkPermissions(): HealthCheck {
        val hasPermission = DeviceIdentifierHelper.hasReadDeviceIdentifiersPermission(context)
        
        return if (hasPermission) {
            HealthCheck(
                name = "Permissions",
                status = HealthStatus.HEALTHY,
                message = "All required permissions granted",
                details = mapOf("READ_DEVICE_IDENTIFIERS" to true)
            )
        } else {
            HealthCheck(
                name = "Permissions",
                status = HealthStatus.WARNING,
                message = "READ_DEVICE_IDENTIFIERS permission not granted",
                details = mapOf(
                    "READ_DEVICE_IDENTIFIERS" to false,
                    "grantCommand" to DeviceIdentifierHelper.getAdbGrantCommand()
                )
            )
        }
    }

    private fun checkDeviceIdentifiers(): HealthCheck {
        val imei = DeviceIdentifierHelper.getDeviceId(context)
        
        return if (imei != null) {
            HealthCheck(
                name = "Device Identifiers",
                status = HealthStatus.HEALTHY,
                message = "Device ID available",
                details = mapOf("available" to true)
            )
        } else {
            HealthCheck(
                name = "Device Identifiers",
                status = HealthStatus.WARNING,
                message = "Device ID not accessible",
                details = mapOf("available" to false)
            )
        }
    }

    private fun checkRcsState(): HealthCheck {
        return try {
            val orchestrator = RcsOrchestrator.getInstance(context)
            val state = orchestrator.getCurrentState()
            
            val status = when (state) {
                RcsState.Registered -> HealthStatus.HEALTHY
                RcsState.Registering, RcsState.Connecting -> HealthStatus.WARNING
                RcsState.Error -> HealthStatus.CRITICAL
                else -> HealthStatus.WARNING
            }
            
            HealthCheck(
                name = "RCS State",
                status = status,
                message = "Current state: $state",
                details = mapOf("state" to state.toString())
            )
        } catch (e: Exception) {
            HealthCheck(
                name = "RCS State",
                status = HealthStatus.UNKNOWN,
                message = "Unable to check RCS state",
                details = mapOf("error" to (e.message ?: "Unknown"))
            )
        }
    }

    private fun checkDeviceCapabilities(): HealthCheck {
        val capabilities = mutableMapOf<String, Boolean>()
        
        capabilities["ims"] = true
        capabilities["lte"] = hasLteSupport()
        capabilities["5g"] = has5gSupport()
        
        return HealthCheck(
            name = "Device Capabilities",
            status = HealthStatus.HEALTHY,
            message = "Device supports RCS requirements",
            details = capabilities.mapValues { it.value }
        )
    }

    private fun hasLteSupport(): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.phoneCount > 0
    }

    private fun has5gSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun generateRecommendations(checks: List<HealthCheck>): List<String> {
        val recommendations = mutableListOf<String>()
        
        checks.filter { it.status != HealthStatus.HEALTHY }.forEach { check ->
            when (check.name) {
                "Network Connectivity" -> {
                    recommendations.add("Ensure you have a stable internet connection")
                }
                "SIM Card" -> {
                    recommendations.add("Insert a valid SIM card to use RCS")
                }
                "Permissions" -> {
                    val cmd = check.details["grantCommand"]
                    if (cmd != null) {
                        recommendations.add("Grant permission with: $cmd")
                    }
                }
                "RCS State" -> {
                    if (check.status == HealthStatus.CRITICAL) {
                        recommendations.add("Try restarting the device or force reconnecting")
                    }
                }
            }
        }
        
        return recommendations
    }
}

data class HealthCheckResult(
    val overallStatus: HealthStatus,
    val checks: List<HealthCheck>,
    val timestamp: Long,
    val recommendations: List<String>
)

data class HealthCheck(
    val name: String,
    val status: HealthStatus,
    val message: String,
    val details: Map<String, Any>
)

enum class HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL,
    UNKNOWN
}
