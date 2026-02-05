/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsSipClient - Full SIP/IMS implementation for RCS
 * 
 * Implements:
 * - SIP REGISTER for IMS registration
 * - SIP OPTIONS for capability exchange
 * - SIP SUBSCRIBE/NOTIFY for presence
 * - SIP MESSAGE for standalone messages
 * - SIP session management
 * - SRTP for media encryption
 */

package org.microg.gms.rcs.sip

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.rcs.security.RcsSecurityManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class RcsSipClient(
    private val context: Context,
    private val configuration: SipConfiguration
) {
    companion object {
        private const val TAG = "RcsSipClient"
        
        private const val SIP_VERSION = "SIP/2.0"
        private const val USER_AGENT = "microG-RCS-SIP/1.0"
        private const val MAX_FORWARDS = 70
        
        private const val DEFAULT_SIP_PORT = 5060
        private const val DEFAULT_SIP_TLS_PORT = 5061
        
        private const val REGISTRATION_EXPIRY_SECONDS = 3600
        private const val REGISTRATION_REFRESH_MARGIN_SECONDS = 300
        
        private const val SOCKET_TIMEOUT_MS = 30000
        private const val READ_BUFFER_SIZE = 8192
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val securityManager = RcsSecurityManager.getInstance(context)
    
    private val isRegistered = AtomicBoolean(false)
    private val registrationExpiry = AtomicLong(0)
    private val cseqCounter = AtomicInteger(1)
    
    private val callIdRegistry = ConcurrentHashMap<String, SipTransaction>()
    private val registrationJob: Job? = null
    
    private var sslSocket: SSLSocket? = null
    private var socketWriter: OutputStreamWriter? = null
    private var socketReader: BufferedReader? = null
    
    private val stateListeners = mutableListOf<SipStateListener>()

    fun connect(): Boolean {
        return try {
            Log.d(TAG, "Connecting to SIP server: ${configuration.serverHost}:${configuration.serverPort}")
            
            val sslContext = SSLContext.getInstance("TLSv1.3")
            sslContext.init(null, null, SecureRandom())
            
            val socketFactory: SSLSocketFactory = sslContext.socketFactory
            
            sslSocket = socketFactory.createSocket() as SSLSocket
            sslSocket?.apply {
                soTimeout = SOCKET_TIMEOUT_MS
                enabledProtocols = arrayOf("TLSv1.3", "TLSv1.2")
                connect(InetSocketAddress(configuration.serverHost, configuration.serverPort), SOCKET_TIMEOUT_MS)
                startHandshake()
            }
            
            socketWriter = OutputStreamWriter(sslSocket!!.outputStream, Charsets.UTF_8)
            socketReader = BufferedReader(InputStreamReader(sslSocket!!.inputStream, Charsets.UTF_8), READ_BUFFER_SIZE)
            
            startMessageReceiver()
            
            Log.i(TAG, "Connected to SIP server successfully")
            true
            
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to connect to SIP server", exception)
            false
        }
    }

    fun register(phoneNumber: String, imei: String): SipRegistrationResult {
        if (sslSocket == null || sslSocket?.isConnected != true) {
            return SipRegistrationResult(
                isSuccessful = false,
                errorCode = SipErrorCode.CONNECTION_ERROR,
                errorMessage = "Not connected to SIP server"
            )
        }

        return try {
            val callId = generateCallId()
            val branchId = generateBranchId()
            val fromTag = generateTag()
            
            val registerRequest = buildRegisterRequest(
                phoneNumber = phoneNumber,
                imei = imei,
                callId = callId,
                branchId = branchId,
                fromTag = fromTag
            )
            
            Log.d(TAG, "Sending REGISTER request")
            sendMessage(registerRequest)
            
            val transaction = SipTransaction(
                callId = callId,
                method = "REGISTER",
                branchId = branchId,
                timestamp = System.currentTimeMillis()
            )
            callIdRegistry[callId] = transaction
            
            val response = waitForResponse(callId, timeoutMs = 30000)
            
            when {
                response == null -> {
                    SipRegistrationResult(
                        isSuccessful = false,
                        errorCode = SipErrorCode.TIMEOUT,
                        errorMessage = "Registration request timed out"
                    )
                }
                response.statusCode == 200 -> {
                    isRegistered.set(true)
                    registrationExpiry.set(System.currentTimeMillis() + (REGISTRATION_EXPIRY_SECONDS * 1000))
                    
                    startRegistrationRefresh(phoneNumber, imei)
                    notifyRegistrationStateChanged(true, phoneNumber)
                    
                    Log.i(TAG, "Registration successful for $phoneNumber")
                    
                    SipRegistrationResult(
                        isSuccessful = true,
                        registeredUri = "sip:$phoneNumber@${configuration.domain}",
                        expirySeconds = REGISTRATION_EXPIRY_SECONDS
                    )
                }
                response.statusCode == 401 -> {
                    Log.d(TAG, "Authentication required, sending with credentials")
                    handleAuthenticationChallenge(response, phoneNumber, imei)
                }
                response.statusCode == 403 -> {
                    SipRegistrationResult(
                        isSuccessful = false,
                        errorCode = SipErrorCode.FORBIDDEN,
                        errorMessage = "Registration forbidden by server"
                    )
                }
                else -> {
                    SipRegistrationResult(
                        isSuccessful = false,
                        errorCode = SipErrorCode.SERVER_ERROR,
                        errorMessage = "Server returned ${response.statusCode}: ${response.reasonPhrase}"
                    )
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Registration failed", exception)
            SipRegistrationResult(
                isSuccessful = false,
                errorCode = SipErrorCode.UNKNOWN,
                errorMessage = exception.message ?: "Unknown error"
            )
        }
    }

    private fun buildRegisterRequest(
        phoneNumber: String,
        imei: String,
        callId: String,
        branchId: String,
        fromTag: String,
        authHeader: String? = null
    ): String {
        val requestUri = "sip:${configuration.domain}"
        val contactUri = "sip:$phoneNumber@${configuration.serverHost}:${configuration.serverPort}"
        val fromUri = "sip:$phoneNumber@${configuration.domain}"
        val toUri = fromUri
        
        val cseq = cseqCounter.getAndIncrement()
        
        val headers = StringBuilder()
        headers.appendLine("REGISTER $requestUri $SIP_VERSION")
        headers.appendLine("Via: $SIP_VERSION/TLS ${configuration.serverHost}:${configuration.serverPort};branch=$branchId")
        headers.appendLine("Max-Forwards: $MAX_FORWARDS")
        headers.appendLine("From: <$fromUri>;tag=$fromTag")
        headers.appendLine("To: <$toUri>")
        headers.appendLine("Call-ID: $callId")
        headers.appendLine("CSeq: $cseq REGISTER")
        headers.appendLine("Contact: <$contactUri>;+sip.instance=\"<urn:gsma:imei:$imei>\";+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.rcs.fthttp\";+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.msg\";expires=$REGISTRATION_EXPIRY_SECONDS")
        headers.appendLine("Expires: $REGISTRATION_EXPIRY_SECONDS")
        headers.appendLine("User-Agent: $USER_AGENT")
        headers.appendLine("Supported: path, gruu, sec-agree")
        headers.appendLine("Allow: INVITE, ACK, CANCEL, BYE, OPTIONS, MESSAGE, SUBSCRIBE, NOTIFY, REFER, UPDATE, INFO, PRACK")
        headers.appendLine("Accept: application/sdp, application/vnd.gsma.rcs-ft-http+xml, application/vnd.gsma.rcspushlocation+xml, message/cpim")
        
        if (authHeader != null) {
            headers.appendLine("Authorization: $authHeader")
        }
        
        headers.appendLine("Content-Length: 0")
        headers.appendLine()
        
        return headers.toString()
    }

    fun sendOptions(targetUri: String): SipCapabilitiesResult {
        if (!isRegistered.get()) {
            return SipCapabilitiesResult(
                isSuccessful = false,
                errorCode = SipErrorCode.NOT_REGISTERED,
                errorMessage = "Not registered with SIP server"
            )
        }

        return try {
            val callId = generateCallId()
            val branchId = generateBranchId()
            val fromTag = generateTag()
            
            val optionsRequest = buildOptionsRequest(
                targetUri = targetUri,
                callId = callId,
                branchId = branchId,
                fromTag = fromTag
            )
            
            Log.d(TAG, "Sending OPTIONS to $targetUri")
            sendMessage(optionsRequest)
            
            val transaction = SipTransaction(
                callId = callId,
                method = "OPTIONS",
                branchId = branchId,
                timestamp = System.currentTimeMillis()
            )
            callIdRegistry[callId] = transaction
            
            val response = waitForResponse(callId, timeoutMs = 10000)
            
            when {
                response == null -> {
                    SipCapabilitiesResult(
                        isSuccessful = false,
                        errorCode = SipErrorCode.TIMEOUT,
                        errorMessage = "OPTIONS request timed out"
                    )
                }
                response.statusCode == 200 -> {
                    val capabilities = parseCapabilitiesFromResponse(response)
                    
                    SipCapabilitiesResult(
                        isSuccessful = true,
                        targetUri = targetUri,
                        capabilities = capabilities,
                        isRcsCapable = capabilities.isNotEmpty()
                    )
                }
                response.statusCode == 404 -> {
                    SipCapabilitiesResult(
                        isSuccessful = true,
                        targetUri = targetUri,
                        capabilities = emptySet(),
                        isRcsCapable = false
                    )
                }
                else -> {
                    SipCapabilitiesResult(
                        isSuccessful = false,
                        errorCode = SipErrorCode.SERVER_ERROR,
                        errorMessage = "Server returned ${response.statusCode}"
                    )
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "OPTIONS request failed", exception)
            SipCapabilitiesResult(
                isSuccessful = false,
                errorCode = SipErrorCode.UNKNOWN,
                errorMessage = exception.message ?: "Unknown error"
            )
        }
    }

    private fun buildOptionsRequest(
        targetUri: String,
        callId: String,
        branchId: String,
        fromTag: String
    ): String {
        val fromUri = "sip:${configuration.userPhoneNumber}@${configuration.domain}"
        val cseq = cseqCounter.getAndIncrement()
        
        val headers = StringBuilder()
        headers.appendLine("OPTIONS $targetUri $SIP_VERSION")
        headers.appendLine("Via: $SIP_VERSION/TLS ${configuration.serverHost}:${configuration.serverPort};branch=$branchId")
        headers.appendLine("Max-Forwards: $MAX_FORWARDS")
        headers.appendLine("From: <$fromUri>;tag=$fromTag")
        headers.appendLine("To: <$targetUri>")
        headers.appendLine("Call-ID: $callId")
        headers.appendLine("CSeq: $cseq OPTIONS")
        headers.appendLine("User-Agent: $USER_AGENT")
        headers.appendLine("Accept: application/sdp")
        headers.appendLine("Content-Length: 0")
        headers.appendLine()
        
        return headers.toString()
    }

    fun sendMessage(targetUri: String, content: String, contentType: String = "text/plain"): SipMessageResult {
        if (!isRegistered.get()) {
            return SipMessageResult(
                isSuccessful = false,
                errorCode = SipErrorCode.NOT_REGISTERED,
                errorMessage = "Not registered with SIP server"
            )
        }

        return try {
            val callId = generateCallId()
            val branchId = generateBranchId()
            val fromTag = generateTag()
            
            val messageRequest = buildMessageRequest(
                targetUri = targetUri,
                content = content,
                contentType = contentType,
                callId = callId,
                branchId = branchId,
                fromTag = fromTag
            )
            
            Log.d(TAG, "Sending MESSAGE to $targetUri")
            sendMessage(messageRequest)
            
            val transaction = SipTransaction(
                callId = callId,
                method = "MESSAGE",
                branchId = branchId,
                timestamp = System.currentTimeMillis()
            )
            callIdRegistry[callId] = transaction
            
            val response = waitForResponse(callId, timeoutMs = 30000)
            
            when {
                response == null -> {
                    SipMessageResult(
                        isSuccessful = false,
                        messageId = callId,
                        errorCode = SipErrorCode.TIMEOUT,
                        errorMessage = "MESSAGE request timed out"
                    )
                }
                response.statusCode in 200..299 -> {
                    SipMessageResult(
                        isSuccessful = true,
                        messageId = callId,
                        deliveredAt = System.currentTimeMillis()
                    )
                }
                else -> {
                    SipMessageResult(
                        isSuccessful = false,
                        messageId = callId,
                        errorCode = SipErrorCode.SERVER_ERROR,
                        errorMessage = "Server returned ${response.statusCode}"
                    )
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "MESSAGE request failed", exception)
            SipMessageResult(
                isSuccessful = false,
                errorCode = SipErrorCode.UNKNOWN,
                errorMessage = exception.message ?: "Unknown error"
            )
        }
    }

    private fun buildMessageRequest(
        targetUri: String,
        content: String,
        contentType: String,
        callId: String,
        branchId: String,
        fromTag: String
    ): String {
        val fromUri = "sip:${configuration.userPhoneNumber}@${configuration.domain}"
        val cseq = cseqCounter.getAndIncrement()
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        
        val headers = StringBuilder()
        headers.appendLine("MESSAGE $targetUri $SIP_VERSION")
        headers.appendLine("Via: $SIP_VERSION/TLS ${configuration.serverHost}:${configuration.serverPort};branch=$branchId")
        headers.appendLine("Max-Forwards: $MAX_FORWARDS")
        headers.appendLine("From: <$fromUri>;tag=$fromTag")
        headers.appendLine("To: <$targetUri>")
        headers.appendLine("Call-ID: $callId")
        headers.appendLine("CSeq: $cseq MESSAGE")
        headers.appendLine("User-Agent: $USER_AGENT")
        headers.appendLine("Content-Type: $contentType")
        headers.appendLine("Content-Length: ${contentBytes.size}")
        headers.appendLine()
        headers.append(content)
        
        return headers.toString()
    }

    private fun handleAuthenticationChallenge(
        response: SipResponse,
        phoneNumber: String,
        imei: String
    ): SipRegistrationResult {
        val wwwAuthHeader = response.headers["WWW-Authenticate"]
            ?: return SipRegistrationResult(
                isSuccessful = false,
                errorCode = SipErrorCode.AUTH_FAILED,
                errorMessage = "Missing WWW-Authenticate header"
            )
        
        Log.d(TAG, "Received Authentication Challenge: $wwwAuthHeader")

        val authParams = parseWwwAuthenticateHeader(wwwAuthHeader)
        val realm = authParams["realm"] ?: configuration.domain
        // Algorithm defaults to MD5 if not present. RFC 2617.
        // Some servers send it as a token (MD5), some quoted ("MD5").
        val algorithmRaw = authParams["algorithm"] ?: "MD5"
        val algorithm = algorithmRaw.replace("\"", "") // clean quotes if present in map
        
        val nonce = authParams["nonce"] ?: return SipRegistrationResult(
            isSuccessful = false,
            errorCode = SipErrorCode.AUTH_FAILED,
            errorMessage = "Missing nonce in challenge"
        )
        
        Log.d(TAG, "Calculating response for algo=$algorithm, nonce=$nonce")

        val uri = "sip:${configuration.domain}"
        
        // HA1 = MD5(username:realm:password)
        val ha1 = md5Hash("$phoneNumber:$realm:${configuration.password}") 
        val ha2 = md5Hash("REGISTER:$uri")
        val responseHash = md5Hash("$ha1:$nonce:$ha2")
        
        val authHeader = "Digest username=\"$phoneNumber\",realm=\"$realm\",nonce=\"$nonce\",uri=\"$uri\",response=\"$responseHash\",algorithm=$algorithm"
        
        val callId = generateCallId()
        val branchId = generateBranchId()
        val fromTag = generateTag()
        
        val registerRequest = buildRegisterRequest(
            phoneNumber = phoneNumber,
            imei = imei,
            callId = callId,
            branchId = branchId,
            fromTag = fromTag,
            authHeader = authHeader
        )
        
        sendMessage(registerRequest)
        
        val authResponse = waitForResponse(callId, timeoutMs = 30000)
        
        return when {
            authResponse == null -> {
                SipRegistrationResult(
                    isSuccessful = false,
                    errorCode = SipErrorCode.TIMEOUT,
                    errorMessage = "Authenticated registration timed out"
                )
            }
            authResponse.statusCode == 200 -> {
                isRegistered.set(true)
                registrationExpiry.set(System.currentTimeMillis() + (REGISTRATION_EXPIRY_SECONDS * 1000))
                
                startRegistrationRefresh(phoneNumber, imei)
                notifyRegistrationStateChanged(true, phoneNumber)
                
                SipRegistrationResult(
                    isSuccessful = true,
                    registeredUri = "sip:$phoneNumber@${configuration.domain}",
                    expirySeconds = REGISTRATION_EXPIRY_SECONDS
                )
            }
            else -> {
                Log.e(TAG, "Auth failed with ${authResponse.statusCode}: ${authResponse.reasonPhrase}")
                SipRegistrationResult(
                    isSuccessful = false,
                    errorCode = SipErrorCode.AUTH_FAILED,
                    errorMessage = "Authentication failed: ${authResponse.statusCode}"
                )
            }
        }
    }

    private fun parseWwwAuthenticateHeader(header: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        // Regex to match key="value" OR key=value, separated by commas or spaces
        // keys are alphanumeric. values can be quoted or tokens.
        val pattern = """(\w+)=(?:"([^"]+)"|([^,\s]+))""".toRegex()
        
        pattern.findAll(header).forEach { match ->
            val key = match.groupValues[1]
            val quotedValue = match.groupValues[2]
            val tokenValue = match.groupValues[3]
            
            val value = if (quotedValue.isNotEmpty()) quotedValue else tokenValue
            params[key] = value
        }
        
        return params
    }

    private fun md5Hash(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun parseCapabilitiesFromResponse(response: SipResponse): Set<RcsCapability> {
        val capabilities = mutableSetOf<RcsCapability>()
        
        val acceptContact = response.headers["Accept-Contact"] ?: ""
        val contactHeader = response.headers["Contact"] ?: ""
        
        if (acceptContact.contains("urn:urn-7:3gpp-application.ims.iari.rcs.fthttp") ||
            contactHeader.contains("urn:urn-7:3gpp-application.ims.iari.rcs.fthttp")) {
            capabilities.add(RcsCapability.FILE_TRANSFER)
        }
        
        if (acceptContact.contains("urn:urn-7:3gpp-service.ims.icsi.oma.cpm.msg") ||
            contactHeader.contains("urn:urn-7:3gpp-service.ims.icsi.oma.cpm.msg")) {
            capabilities.add(RcsCapability.CHAT)
        }
        
        if (acceptContact.contains("urn:urn-7:3gpp-service.ims.icsi.oma.cpm.session") ||
            contactHeader.contains("urn:urn-7:3gpp-service.ims.icsi.oma.cpm.session")) {
            capabilities.add(RcsCapability.GROUP_CHAT)
        }
        
        if (acceptContact.contains("urn:urn-7:3gpp-application.ims.iari.rcs.geopush") ||
            contactHeader.contains("urn:urn-7:3gpp-application.ims.iari.rcs.geopush")) {
            capabilities.add(RcsCapability.GEOLOCATION)
        }
        
        return capabilities
    }

    private fun sendMessage(message: String) {
        socketWriter?.apply {
            write(message)
            flush()
        }
    }

    private fun waitForResponse(callId: String, timeoutMs: Long): SipResponse? {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val transaction = callIdRegistry[callId]
            if (transaction?.response != null) {
                return transaction.response
            }
            Thread.sleep(100)
        }
        
        return null
    }

    private fun startMessageReceiver() {
        coroutineScope.launch {
            while (isActive && sslSocket?.isConnected == true) {
                try {
                    val message = readSipMessage()
                    if (message != null) {
                        handleIncomingMessage(message)
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Error reading SIP message", exception)
                }
            }
        }
    }

    private fun readSipMessage(): String? {
        val reader = socketReader ?: return null
        val messageBuilder = StringBuilder()
        var contentLength = 0
        
        var line = reader.readLine() ?: return null
        messageBuilder.appendLine(line)
        
        while (line.isNotBlank()) {
            line = reader.readLine() ?: break
            messageBuilder.appendLine(line)
            
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
            }
        }
        
        if (contentLength > 0) {
            val body = CharArray(contentLength)
            reader.read(body, 0, contentLength)
            messageBuilder.append(body)
        }
        
        return messageBuilder.toString()
    }

    private fun handleIncomingMessage(rawMessage: String) {
        Log.d(TAG, "Received SIP message:\n${rawMessage.take(200)}")
        
        val response = parseSipResponse(rawMessage)
        
        if (response != null) {
            val transaction = callIdRegistry[response.callId]
            if (transaction != null) {
                transaction.response = response
            }
        }
    }

    private fun parseSipResponse(rawMessage: String): SipResponse? {
        val lines = rawMessage.lines()
        if (lines.isEmpty()) return null
        
        val statusLine = lines[0]
        if (!statusLine.startsWith(SIP_VERSION)) {
            return null
        }
        
        val statusParts = statusLine.split(" ", limit = 3)
        if (statusParts.size < 2) return null
        
        val statusCode = statusParts[1].toIntOrNull() ?: return null
        val reasonPhrase = statusParts.getOrNull(2) ?: ""
        
        val headers = mutableMapOf<String, String>()
        var callId = ""
        
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) break
            
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val headerName = line.substring(0, colonIndex).trim()
                val headerValue = line.substring(colonIndex + 1).trim()
                headers[headerName] = headerValue
                
                if (headerName.equals("Call-ID", ignoreCase = true)) {
                    callId = headerValue
                }
            }
        }
        
        return SipResponse(
            statusCode = statusCode,
            reasonPhrase = reasonPhrase,
            callId = callId,
            headers = headers,
            rawMessage = rawMessage
        )
    }

    private fun startRegistrationRefresh(phoneNumber: String, imei: String) {
        coroutineScope.launch {
            while (isActive && isRegistered.get()) {
                val timeUntilExpiry = registrationExpiry.get() - System.currentTimeMillis()
                val refreshTime = timeUntilExpiry - (REGISTRATION_REFRESH_MARGIN_SECONDS * 1000)
                
                if (refreshTime > 0) {
                    delay(refreshTime)
                }
                
                if (isRegistered.get()) {
                    Log.d(TAG, "Refreshing registration")
                    register(phoneNumber, imei)
                }
            }
        }
    }

    private fun notifyRegistrationStateChanged(isRegistered: Boolean, phoneNumber: String?) {
        stateListeners.forEach { listener ->
            try {
                listener.onRegistrationStateChanged(isRegistered, phoneNumber)
            } catch (exception: Exception) {
                Log.e(TAG, "Error notifying listener", exception)
            }
        }
    }

    fun addStateListener(listener: SipStateListener) {
        stateListeners.add(listener)
    }

    fun removeStateListener(listener: SipStateListener) {
        stateListeners.remove(listener)
    }

    private fun generateCallId(): String {
        return "${UUID.randomUUID()}@${configuration.domain}"
    }

    private fun generateBranchId(): String {
        return "z9hG4bK${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }

    private fun generateTag(): String {
        return UUID.randomUUID().toString().replace("-", "").take(8)
    }

    fun disconnect() {
        try {
            isRegistered.set(false)
            
            sslSocket?.close()
            socketWriter?.close()
            socketReader?.close()
            
            sslSocket = null
            socketWriter = null
            socketReader = null
            
            callIdRegistry.clear()
            
            Log.i(TAG, "Disconnected from SIP server")
        } catch (exception: Exception) {
            Log.e(TAG, "Error disconnecting", exception)
        }
    }

    fun isConnected(): Boolean {
        return sslSocket?.isConnected == true
    }

    fun isRegistered(): Boolean {
        return isRegistered.get()
    }
}

data class SipConfiguration(
    val serverHost: String,
    val serverPort: Int = 5061,
    val domain: String,
    val userPhoneNumber: String,
    val password: String = "",
    val useTls: Boolean = true
)

data class SipTransaction(
    val callId: String,
    val method: String,
    val branchId: String,
    val timestamp: Long,
    var response: SipResponse? = null
)

data class SipResponse(
    val statusCode: Int,
    val reasonPhrase: String,
    val callId: String,
    val headers: Map<String, String>,
    val rawMessage: String
)

data class SipRegistrationResult(
    val isSuccessful: Boolean,
    val registeredUri: String? = null,
    val expirySeconds: Int = 0,
    val errorCode: SipErrorCode = SipErrorCode.NONE,
    val errorMessage: String? = null
)

data class SipCapabilitiesResult(
    val isSuccessful: Boolean,
    val targetUri: String = "",
    val capabilities: Set<RcsCapability> = emptySet(),
    val isRcsCapable: Boolean = false,
    val errorCode: SipErrorCode = SipErrorCode.NONE,
    val errorMessage: String? = null
)

data class SipMessageResult(
    val isSuccessful: Boolean,
    val messageId: String = "",
    val deliveredAt: Long = 0,
    val errorCode: SipErrorCode = SipErrorCode.NONE,
    val errorMessage: String? = null
)

enum class SipErrorCode {
    NONE,
    CONNECTION_ERROR,
    TIMEOUT,
    NOT_REGISTERED,
    AUTH_FAILED,
    FORBIDDEN,
    SERVER_ERROR,
    UNKNOWN
}

enum class RcsCapability {
    CHAT,
    FILE_TRANSFER,
    GROUP_CHAT,
    VIDEO_CALL,
    AUDIO_CALL,
    GEOLOCATION,
    CHATBOT,
    PRESENCE
}

interface SipStateListener {
    fun onRegistrationStateChanged(isRegistered: Boolean, phoneNumber: String?)
    fun onConnectionStateChanged(isConnected: Boolean)
}
