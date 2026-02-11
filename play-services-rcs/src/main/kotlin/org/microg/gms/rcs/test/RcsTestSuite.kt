/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsTestSuite - Integration test runner for RCS components
 */

package org.microg.gms.rcs.test

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.microg.gms.rcs.carrier.ExtendedCarrierDatabase
import org.microg.gms.rcs.error.RcsErrorHandler
import org.microg.gms.rcs.events.RcsEventBus
import org.microg.gms.rcs.protocol.CpimMessageParser
import org.microg.gms.rcs.protocol.ImdnMessageParser
import org.microg.gms.rcs.retry.RetryPolicies
import org.microg.gms.rcs.retry.RetryResult
import org.microg.gms.rcs.security.RcsSecurityManager
import org.microg.gms.rcs.state.RcsEvent
import org.microg.gms.rcs.state.RcsState
import org.microg.gms.rcs.state.RcsStateMachine
import org.microg.gms.rcs.validation.RcsValidator
import org.microg.gms.rcs.validation.ValidationResult
import java.util.Date

class RcsTestSuite(private val context: Context) {

    private val results = mutableListOf<TestResult>()

    fun runAllTests(): TestSuiteResult {
        results.clear()
        
        runValidatorTests()
        runStateMachineTests()
        runCpimParserTests()
        runImdnParserTests()
        runSecurityTests()
        runCarrierDatabaseTests()
        runRetryPolicyTests()
        
        return TestSuiteResult(
            totalTests = results.size,
            passed = results.count { it.passed },
            failed = results.count { !it.passed },
            results = results.toList()
        )
    }

    private fun test(name: String, block: () -> Unit) {
        val result = try {
            block()
            TestResult(name, true, null)
        } catch (e: AssertionError) {
            TestResult(name, false, e.message)
        } catch (e: Exception) {
            TestResult(name, false, "Exception: ${e.message}")
        }
        results.add(result)
    }

    private fun runValidatorTests() {
        test("Validator: Valid phone number") {
            val result = RcsValidator.validatePhoneNumber("+14155551234")
            assert(result is ValidationResult.Valid)
        }
        
        test("Validator: Invalid phone number") {
            val result = RcsValidator.validatePhoneNumber("abc")
            assert(result is ValidationResult.Invalid)
        }
        
        test("Validator: Valid E.164") {
            val result = RcsValidator.validateE164PhoneNumber("+14155551234")
            assert(result is ValidationResult.Valid)
        }
        
        test("Validator: Valid IMEI with Luhn checksum") {
            val result = RcsValidator.validateImei("490154203237518")
            assert(result is ValidationResult.Valid)
        }
        
        test("Validator: Message content length") {
            val longMessage = "a".repeat(200000)
            val result = RcsValidator.validateMessageContent(longMessage)
            assert(result is ValidationResult.Invalid)
        }
        
        test("Validator: File size limit") {
            val result = RcsValidator.validateFileSize(200 * 1024 * 1024, 100)
            assert(result is ValidationResult.Invalid)
        }
        
        test("Validator: Valid group name") {
            val result = RcsValidator.validateGroupName("My Group")
            assert(result is ValidationResult.Valid)
        }
    }

    private fun runStateMachineTests() {
        test("StateMachine: Initial state is Uninitialized") {
            val sm = RcsStateMachine()
            assert(sm.getCurrentState() == RcsState.Uninitialized)
        }
        
        test("StateMachine: Valid transition Initialize -> Initializing") {
            val sm = RcsStateMachine()
            runBlocking {
                val result = sm.processEvent(RcsEvent.Initialize)
                assert(sm.getCurrentState() == RcsState.Initializing)
            }
        }
        
        test("StateMachine: Invalid transition rejected") {
            val sm = RcsStateMachine()
            runBlocking {
                val canProcess = sm.canProcessEvent(RcsEvent.RegistrationComplete)
                assert(!canProcess)
            }
        }
        
        test("StateMachine: Full registration flow") {
            val sm = RcsStateMachine()
            runBlocking {
                sm.processEvent(RcsEvent.Initialize)
                sm.processEvent(RcsEvent.InitializationComplete)
                sm.processEvent(RcsEvent.Connect)
                sm.processEvent(RcsEvent.ConnectionEstablished)
                sm.processEvent(RcsEvent.StartRegistration)
                sm.processEvent(RcsEvent.RegistrationComplete)
                
                assert(sm.getCurrentState() == RcsState.Registered)
            }
        }
    }

    private fun runCpimParserTests() {
        test("CPIM: Parse simple message") {
            val raw = """
                From: <sip:user@example.com>
                To: <sip:recipient@example.com>
                DateTime: 2024-01-15T10:30:00Z

                Content-Type: text/plain

                Hello World
            """.trimIndent().replace("\n", "\r\n")
            
            val message = CpimMessageParser.parse(raw)
            assert(message != null)
            assert(message?.body == "Hello World")
        }
        
        test("CPIM: Build simple message") {
            val cpim = CpimMessageParser.buildSimple(
                from = "sip:sender@example.com",
                to = "sip:recipient@example.com",
                content = "Test message"
            )
            
            assert(cpim.contains("From:"))
            assert(cpim.contains("To:"))
            assert(cpim.contains("Test message"))
        }
        
        test("CPIM: Extract phone number from SIP URI") {
            val message = CpimMessageParser.parse(
                "From: <sip:+14155551234@example.com>\r\n\r\nContent-Type: text/plain\r\n\r\nHi"
            )
            
            val phone = message?.getFromPhoneNumber()
            assert(phone == "+14155551234")
        }
    }

    private fun runImdnParserTests() {
        test("IMDN: Parse delivered notification") {
            val xml = """
                <?xml version="1.0"?>
                <imdn xmlns="urn:ietf:params:imdn">
                    <message-id>abc123</message-id>
                    <datetime>2024-01-15T10:30:00Z</datetime>
                    <delivery-notification>
                        <status><delivered/></status>
                    </delivery-notification>
                </imdn>
            """.trimIndent()
            
            val notification = ImdnMessageParser.parseDispositionNotification(xml)
            assert(notification?.messageId == "abc123")
            assert(notification?.status == org.microg.gms.rcs.protocol.ImdnStatus.DELIVERED)
        }
        
        test("IMDN: Build delivered notification") {
            val xml = ImdnMessageParser.buildDeliveredNotification("msg-123", "sip:user@example.com")
            assert(xml.contains("msg-123"))
            assert(xml.contains("<delivered/>"))
        }
    }

    private fun runSecurityTests() {
        test("Security: Encrypt and decrypt data") {
            val security = RcsSecurityManager.getInstance(context)
            val original = "Sensitive data to encrypt"
            
            val encrypted = security.encryptData(original)
            val decrypted = security.decryptDataToString(encrypted)
            
            assert(decrypted == original)
        }
        
        test("Security: Generate random bytes") {
            val security = RcsSecurityManager.getInstance(context)
            val random1 = security.generateSecureRandomBytes(32)
            val random2 = security.generateSecureRandomBytes(32)
            
            assert(random1.size == 32)
            assert(!random1.contentEquals(random2))
        }
        
        test("Security: HMAC verification") {
            val security = RcsSecurityManager.getInstance(context)
            val data = "Data to verify".toByteArray()
            
            val hmac = security.generateHmac(data)
            val isValid = security.verifyHmac(data, hmac)
            
            assert(isValid)
        }
    }

    private fun runCarrierDatabaseTests() {
        test("CarrierDB: T-Mobile US exists") {
            val config = ExtendedCarrierDatabase.getConfig("310260")
            assert(config.carrierName == "T-Mobile US")
        }
        
        test("CarrierDB: Unknown carrier uses Jibe") {
            val config = ExtendedCarrierDatabase.getConfig("999999")
            assert(config.carrierName == "Google Jibe")
        }
        
        test("CarrierDB: Has multiple carriers") {
            val carriers = ExtendedCarrierDatabase.getAllSupportedCarriers()
            assert(carriers.size > 40)
        }
    }

    private fun runRetryPolicyTests() {
        test("RetryPolicy: Successful first attempt") {
            runBlocking {
                val result = RetryPolicies.quickRetry.execute {
                    "success"
                }
                
                assert(result is RetryResult.Success)
                assert((result as RetryResult.Success).attempts == 1)
            }
        }
        
        test("RetryPolicy: Failure after max attempts") {
            runBlocking {
                var attempts = 0
                val result = RetryPolicies.quickRetry.execute {
                    attempts++
                    throw Exception("Always fails")
                }
                
                assert(result is RetryResult.Failure)
                assert(attempts == 2)
            }
        }
    }
}

data class TestResult(
    val name: String,
    val passed: Boolean,
    val errorMessage: String?
)

data class TestSuiteResult(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val results: List<TestResult>
) {
    fun toReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=" .repeat(60))
        sb.appendLine("RCS TEST SUITE RESULTS")
        sb.appendLine("=".repeat(60))
        sb.appendLine("Total: $totalTests | Passed: $passed | Failed: $failed")
        sb.appendLine()
        
        results.forEach { r ->
            val icon = if (r.passed) "✓" else "✗"
            sb.appendLine("[$icon] ${r.name}")
            if (!r.passed && r.errorMessage != null) {
                sb.appendLine("    Error: ${r.errorMessage}")
            }
        }
        
        return sb.toString()
    }
}
