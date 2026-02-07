/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsSecurityAudit - Security monitoring and compliance
 */

package org.microg.gms.rcs.security

import android.content.Context
import android.util.Log
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RcsSecurityAudit private constructor() {

    companion object {
        private const val TAG = "RcsSecurityAudit"
        
        @Volatile
        private var instance: RcsSecurityAudit? = null
        
        fun getInstance(): RcsSecurityAudit {
            return instance ?: synchronized(this) {
                instance ?: RcsSecurityAudit().also { instance = it }
            }
        }
    }

    private val auditLog = CopyOnWriteArrayList<SecurityAuditEntry>()
    private val securityListeners = CopyOnWriteArrayList<SecurityAuditListener>()

    fun logSecurityEvent(event: SecurityEvent) {
        val entry = SecurityAuditEntry(
            timestamp = System.currentTimeMillis(),
            event = event,
            details = event.getDetails()
        )
        
        auditLog.add(entry)
        
        if (auditLog.size > 1000) {
            auditLog.removeAt(0)
        }
        
        Log.d(TAG, "Security event: ${event.type} - ${event.getDetails()}")
        
        if (event.severity == SecuritySeverity.CRITICAL) {
            notifyListeners(entry)
        }
    }

    fun getAuditLog(lastN: Int = 100): List<SecurityAuditEntry> {
        return auditLog.takeLast(lastN)
    }

    fun getSecurityScore(): SecurityScore {
        val recentEvents = auditLog.filter { 
            System.currentTimeMillis() - it.timestamp < 24 * 60 * 60 * 1000 
        }
        
        val criticalCount = recentEvents.count { it.event.severity == SecuritySeverity.CRITICAL }
        val warningCount = recentEvents.count { it.event.severity == SecuritySeverity.WARNING }
        
        val score = when {
            criticalCount > 0 -> 0
            warningCount > 10 -> 50
            warningCount > 5 -> 70
            warningCount > 0 -> 85
            else -> 100
        }
        
        return SecurityScore(
            score = score,
            rating = when {
                score >= 90 -> SecurityRating.EXCELLENT
                score >= 70 -> SecurityRating.GOOD
                score >= 50 -> SecurityRating.FAIR
                else -> SecurityRating.POOR
            },
            criticalIssues = criticalCount,
            warnings = warningCount
        )
    }

    fun addListener(listener: SecurityAuditListener) {
        securityListeners.add(listener)
    }

    fun removeListener(listener: SecurityAuditListener) {
        securityListeners.remove(listener)
    }

    private fun notifyListeners(entry: SecurityAuditEntry) {
        securityListeners.forEach { listener ->
            try {
                listener.onSecurityEvent(entry)
            } catch (e: Exception) {
                Log.e(TAG, "Security listener error", e)
            }
        }
    }

    fun clearLog() {
        auditLog.clear()
    }
}

sealed class SecurityEvent {
    abstract val type: String
    abstract val severity: SecuritySeverity
    abstract fun getDetails(): String
    
    data class EncryptionUsed(
        val algorithm: String,
        val keySize: Int
    ) : SecurityEvent() {
        override val type = "ENCRYPTION_USED"
        override val severity = SecuritySeverity.INFO
        override fun getDetails() = "Algorithm: $algorithm, Key size: $keySize bits"
    }
    
    data class TlsConnectionEstablished(
        val host: String,
        val protocol: String,
        val cipherSuite: String
    ) : SecurityEvent() {
        override val type = "TLS_ESTABLISHED"
        override val severity = SecuritySeverity.INFO
        override fun getDetails() = "Host: $host, Protocol: $protocol, Cipher: $cipherSuite"
    }
    
    data class CertificatePinningSuccess(val host: String) : SecurityEvent() {
        override val type = "CERT_PIN_SUCCESS"
        override val severity = SecuritySeverity.INFO
        override fun getDetails() = "Certificate pinning verified for $host"
    }
    
    data class CertificatePinningFailure(
        val host: String,
        val reason: String
    ) : SecurityEvent() {
        override val type = "CERT_PIN_FAILURE"
        override val severity = SecuritySeverity.CRITICAL
        override fun getDetails() = "Certificate pinning failed for $host: $reason"
    }
    
    data class AuthenticationSuccess(val method: String) : SecurityEvent() {
        override val type = "AUTH_SUCCESS"
        override val severity = SecuritySeverity.INFO
        override fun getDetails() = "Authentication successful via $method"
    }
    
    data class AuthenticationFailure(
        val method: String,
        val reason: String
    ) : SecurityEvent() {
        override val type = "AUTH_FAILURE"
        override val severity = SecuritySeverity.WARNING
        override fun getDetails() = "Authentication failed via $method: $reason"
    }
    
    data class SuspiciousActivity(
        val description: String,
        val source: String
    ) : SecurityEvent() {
        override val type = "SUSPICIOUS_ACTIVITY"
        override val severity = SecuritySeverity.WARNING
        override fun getDetails() = "Suspicious activity from $source: $description"
    }
    
    data class KeyRotation(val keyAlias: String) : SecurityEvent() {
        override val type = "KEY_ROTATION"
        override val severity = SecuritySeverity.INFO
        override fun getDetails() = "Key rotated: $keyAlias"
    }
    
    data class DataWipe(val component: String) : SecurityEvent() {
        override val type = "DATA_WIPE"
        override val severity = SecuritySeverity.INFO
        override fun getDetails() = "Secure data wipe for $component"
    }
}

enum class SecuritySeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class SecurityAuditEntry(
    val timestamp: Long,
    val event: SecurityEvent,
    val details: String
)

data class SecurityScore(
    val score: Int,
    val rating: SecurityRating,
    val criticalIssues: Int,
    val warnings: Int
)

enum class SecurityRating {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

interface SecurityAuditListener {
    fun onSecurityEvent(entry: SecurityAuditEntry)
}

class CertificateValidator {
    
    fun validateCertificateChain(chain: Array<X509Certificate>): CertificateValidationResult {
        if (chain.isEmpty()) {
            return CertificateValidationResult.Invalid("Empty certificate chain")
        }
        
        val leaf = chain[0]
        
        val now = Date()
        if (now.before(leaf.notBefore)) {
            return CertificateValidationResult.Invalid("Certificate not yet valid")
        }
        if (now.after(leaf.notAfter)) {
            return CertificateValidationResult.Invalid("Certificate expired")
        }
        
        try {
            leaf.checkValidity()
        } catch (e: Exception) {
            return CertificateValidationResult.Invalid("Certificate validation failed: ${e.message}")
        }
        
        val daysUntilExpiry = (leaf.notAfter.time - now.time) / (24 * 60 * 60 * 1000)
        if (daysUntilExpiry < 30) {
            return CertificateValidationResult.Warning("Certificate expires in $daysUntilExpiry days")
        }
        
        return CertificateValidationResult.Valid(
            subject = leaf.subjectDN.name,
            issuer = leaf.issuerDN.name,
            validUntil = leaf.notAfter
        )
    }
}

sealed class CertificateValidationResult {
    data class Valid(
        val subject: String,
        val issuer: String,
        val validUntil: Date
    ) : CertificateValidationResult()
    
    data class Warning(val message: String) : CertificateValidationResult()
    data class Invalid(val reason: String) : CertificateValidationResult()
}
