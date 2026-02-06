/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsDebugTools - Debugging and troubleshooting utilities
 */

package org.microg.gms.rcs.debug

import android.content.Context
import android.os.Build
import org.microg.gms.rcs.carrier.ExtendedCarrierDatabase
import org.microg.gms.rcs.config.RcsConfigManager
import org.microg.gms.rcs.health.HealthStatus
import org.microg.gms.rcs.health.RcsHealthChecker
import org.microg.gms.rcs.logging.RcsLogger
import org.microg.gms.rcs.metrics.RcsMetricsCollector
import org.microg.gms.rcs.orchestrator.RcsOrchestrator
import org.microg.gms.rcs.SimCardHelper
import org.microg.gms.rcs.error.RcsErrorHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RcsDebugTools(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    suspend fun generateDiagnosticReport(): String {
        val report = StringBuilder()
        
        report.appendLine("=" .repeat(60))
        report.appendLine("RCS DIAGNOSTIC REPORT")
        report.appendLine("Generated: ${dateFormat.format(Date())}")
        report.appendLine("=".repeat(60))
        report.appendLine()
        
        appendDeviceInfo(report)
        appendSimInfo(report)
        appendCarrierInfo(report)
        appendRcsState(report)
        appendHealthCheck(report)
        appendMetrics(report)
        appendRecentErrors(report)
        appendConfiguration(report)
        appendLogs(report)
        
        report.appendLine("=".repeat(60))
        report.appendLine("END OF REPORT")
        report.appendLine("=".repeat(60))
        
        return report.toString()
    }

    private fun appendDeviceInfo(report: StringBuilder) {
        report.appendLine("## DEVICE INFO")
        report.appendLine("-".repeat(40))
        report.appendLine("Model: ${Build.MODEL}")
        report.appendLine("Manufacturer: ${Build.MANUFACTURER}")
        report.appendLine("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        report.appendLine("Device: ${Build.DEVICE}")
        report.appendLine("Product: ${Build.PRODUCT}")
        report.appendLine("Build ID: ${Build.ID}")
        report.appendLine()
    }

    private fun appendSimInfo(report: StringBuilder) {
        val isReady = SimCardHelper.isSimCardReady(context)
        val simInfo = SimCardHelper.getSimCardInfo(context)
        
        report.appendLine("## SIM CARD INFO")
        report.appendLine("-".repeat(40))
        report.appendLine("SIM Ready: $isReady")
        report.appendLine("MCC/MNC: ${simInfo?.mccMnc ?: "N/A"}")
        report.appendLine("Carrier: ${simInfo?.carrierName ?: "N/A"}")
        report.appendLine("Phone Number: ${maskPhoneNumber(simInfo?.phoneNumber)}")
        report.appendLine()
    }

    private fun appendCarrierInfo(report: StringBuilder) {
        val simInfo = SimCardHelper.getSimCardInfo(context)
        val mccMnc = simInfo?.mccMnc
        
        report.appendLine("## CARRIER RCS CONFIG")
        report.appendLine("-".repeat(40))
        
        if (mccMnc != null) {
            val config = ExtendedCarrierDatabase.getConfig(mccMnc)
            val isNative = ExtendedCarrierDatabase.isCarrierSupported(mccMnc)
            
            report.appendLine("Carrier Database Match: $isNative")
            report.appendLine("RCS Server: ${config.rcsServer}")
            report.appendLine("RCS Port: ${config.rcsPort}")
            report.appendLine("RCS Profile: ${config.rcsProfile}")
            report.appendLine("Supports Jibe: ${config.supportsJibe}")
            report.appendLine("File Transfer Max: ${config.maxFileTransferSize / (1024 * 1024)}MB")
        } else {
            report.appendLine("Cannot determine carrier configuration")
        }
        report.appendLine()
    }

    private fun appendRcsState(report: StringBuilder) {
        report.appendLine("## RCS SERVICE STATE")
        report.appendLine("-".repeat(40))
        
        try {
            val orchestrator = RcsOrchestrator.getInstance(context)
            report.appendLine("Current State: ${orchestrator.getCurrentState()}")
            report.appendLine("Is Registered: ${orchestrator.isRegistered()}")
            report.appendLine("Is Connected: ${orchestrator.isConnected()}")
            report.appendLine("Phone Number: ${maskPhoneNumber(orchestrator.getPhoneNumber())}")
        } catch (e: Exception) {
            report.appendLine("Error getting RCS state: ${e.message}")
        }
        report.appendLine()
    }

    private suspend fun appendHealthCheck(report: StringBuilder) {
        report.appendLine("## HEALTH CHECK")
        report.appendLine("-".repeat(40))
        
        try {
            val healthChecker = RcsHealthChecker(context)
            val result = healthChecker.performHealthCheck()
            
            report.appendLine("Overall Status: ${result.overallStatus}")
            report.appendLine()
            
            result.checks.forEach { check ->
                val statusIcon = when (check.status) {
                    HealthStatus.HEALTHY -> "✓"
                    HealthStatus.WARNING -> "!"
                    HealthStatus.CRITICAL -> "✗"
                    HealthStatus.UNKNOWN -> "?"
                }
                report.appendLine("[$statusIcon] ${check.name}: ${check.message}")
            }
            
            if (result.recommendations.isNotEmpty()) {
                report.appendLine()
                report.appendLine("Recommendations:")
                result.recommendations.forEach { rec ->
                    report.appendLine("  • $rec")
                }
            }
        } catch (e: Exception) {
            report.appendLine("Error performing health check: ${e.message}")
        }
        report.appendLine()
    }

    private fun appendMetrics(report: StringBuilder) {
        report.appendLine("## METRICS")
        report.appendLine("-".repeat(40))
        
        try {
            val metrics = RcsMetricsCollector.getInstance(context)
            val snapshot = metrics.getAllMetrics()
            
            report.appendLine("Counters:")
            snapshot.counters.forEach { (name, value) ->
                report.appendLine("  $name: $value")
            }
            
            report.appendLine()
            report.appendLine("Gauges:")
            snapshot.gauges.forEach { (name, value) ->
                report.appendLine("  $name: $value")
            }
            
            report.appendLine()
            report.appendLine("Timings:")
            snapshot.timings.forEach { (name, stats) ->
                report.appendLine("  $name: avg=${stats.avgMs}ms, min=${stats.minMs}ms, max=${stats.maxMs}ms, count=${stats.count}")
            }
        } catch (e: Exception) {
            report.appendLine("Error getting metrics: ${e.message}")
        }
        report.appendLine()
    }

    private fun appendRecentErrors(report: StringBuilder) {
        report.appendLine("## RECENT ERRORS")
        report.appendLine("-".repeat(40))
        
        try {
            val errorStats = RcsErrorHandler.getInstance().getErrorStatistics()
            
            report.appendLine("Total Errors: ${errorStats.totalErrors}")
            report.appendLine()
            report.appendLine("By Category:")
            errorStats.errorsByCategory.forEach { (cat, count) ->
                report.appendLine("  $cat: $count")
            }
            
            if (errorStats.recentErrors.isNotEmpty()) {
                report.appendLine()
                report.appendLine("Last 10 Errors:")
                errorStats.recentErrors.forEach { error ->
                    val time = dateFormat.format(Date(error.timestamp))
                    report.appendLine("  [$time] [${error.component}] ${error.code}: ${error.message}")
                }
            }
        } catch (e: Exception) {
            report.appendLine("Error getting error stats: ${e.message}")
        }
        report.appendLine()
    }

    private fun appendConfiguration(report: StringBuilder) {
        report.appendLine("## CONFIGURATION")
        report.appendLine("-".repeat(40))
        
        try {
            val config = RcsConfigManager.getInstance(context)
            report.appendLine(config.exportConfig())
        } catch (e: Exception) {
            report.appendLine("Error getting configuration: ${e.message}")
        }
        report.appendLine()
    }

    private fun appendLogs(report: StringBuilder) {
        report.appendLine("## RECENT LOGS")
        report.appendLine("-".repeat(40))
        
        val logs = RcsLogger.getLogHistory(limit = 50)
        
        logs.forEach { entry ->
            val time = dateFormat.format(Date(entry.timestamp))
            report.appendLine("[$time] [${entry.level}] [${entry.tag}] ${entry.message}")
        }
        report.appendLine()
    }

    private fun maskPhoneNumber(phone: String?): String {
        if (phone == null || phone.length < 4) return "N/A"
        return "*".repeat(phone.length - 4) + phone.takeLast(4)
    }

    fun clearAllData() {
        RcsLogger.clearLogs()
        RcsErrorHandler.getInstance().clearErrorHistory()
        RcsMetricsCollector.getInstance(context).reset()
        RcsConfigManager.getInstance(context).resetToDefaults()
    }
}
