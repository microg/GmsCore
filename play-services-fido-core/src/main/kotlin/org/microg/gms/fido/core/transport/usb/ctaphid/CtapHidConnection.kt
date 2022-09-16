package org.microg.gms.fido.core.transport.usb.ctaphid

import android.content.Context
import android.hardware.usb.UsbConstants.USB_DIR_IN
import android.hardware.usb.UsbConstants.USB_DIR_OUT
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbRequest
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.microg.gms.fido.core.protocol.msgs.*
import org.microg.gms.fido.core.transport.CtapConnection
import org.microg.gms.fido.core.transport.usb.endpoints
import org.microg.gms.fido.core.transport.usb.initialize
import org.microg.gms.fido.core.transport.usb.usbManager
import org.microg.gms.fido.core.transport.usb.use
import org.microg.gms.utils.toBase64
import java.nio.ByteBuffer
import kotlin.experimental.and

class CtapHidConnection(
    val context: Context,
    val device: UsbDevice,
    val iface: UsbInterface,
) : CtapConnection {
    private var connection: UsbDeviceConnection? = null
    private val inEndpoint = iface.endpoints.first { it.direction == USB_DIR_IN }
    private val outEndpoint = iface.endpoints.first { it.direction == USB_DIR_OUT }
    private var channelIdentifier = 0xffffffff.toInt()
    private var capabilities: Byte = 0

    override val hasCtap1Support: Boolean
        get() = capabilities and CtapHidInitResponse.CAPABILITY_NMSG == 0.toByte()

    override val hasCtap2Support: Boolean
        get() = capabilities and CtapHidInitResponse.CAPABILITY_CBOR > 0

    val hasWinkSupport: Boolean
        get() = capabilities and CtapHidInitResponse.CAPABILITY_WINK > 0

    suspend fun open(): Boolean {
        Log.d(TAG, "Opening connection")
        connection = context.usbManager?.openDevice(device)
        if (connection?.claimInterface(iface, true) != true) {
            Log.d(TAG, "Failed claiming interface")
            close()
            return false
        }
        val initRequest = CtapHidInitRequest()
        sendRequest(initRequest)
        val initResponse = readResponse()
        if (initResponse !is CtapHidInitResponse || !initResponse.nonce.contentEquals(initRequest.nonce)) {
            Log.d(TAG, "Failed init procedure")
            close()
            return false
        }
        channelIdentifier = initResponse.channelId
        capabilities = initResponse.capabilities
        return true
    }

    suspend fun close() {
        connection?.close()
        connection = null
        channelIdentifier = 0xffffffff.toInt()
        capabilities = 0
    }

    suspend fun sendRequest(request: CtapHidRequest) {
        val connection = connection ?: throw IllegalStateException("Not opened")
        val packets = request.encodePackets(channelIdentifier, outEndpoint.maxPacketSize)
        Log.d(TAG, "Sending $request in ${packets.size} packets")
        UsbRequest().initialize(connection, outEndpoint) { outRequest ->
            for (packet in packets) {
                if (outRequest.queue(ByteBuffer.wrap(packet.bytes), packet.bytes.size)) {
                    withContext(Dispatchers.IO) { connection.requestWait() }
                    Log.d(TAG, "Sent packet ${packet.bytes.toBase64(Base64.NO_WRAP)}")
                } else {
                    throw RuntimeException("Failed queuing packet")
                }
            }
        }
    }

    suspend fun readResponse(timeout: Long = 1000): CtapHidResponse = withTimeout(timeout) {
        val connection = connection ?: throw IllegalStateException("Not opened")
        UsbRequest().initialize(connection, inEndpoint) { inRequest ->
            val packets = mutableListOf<CtapHidPacket>()
            val buffer = ByteBuffer.allocate(inEndpoint.maxPacketSize)
            var initializationPacket: CtapHidInitializationPacket? = null
            while (true) {
                buffer.clear()
                if (inRequest.queue(buffer, inEndpoint.maxPacketSize)) {
                    Log.d(TAG, "Reading ${inEndpoint.maxPacketSize} bytes from usb")
                    withContext(Dispatchers.IO) { connection.requestWait() }
                    Log.d(TAG, "Received packet ${buffer.array().toBase64(Base64.NO_WRAP)}")
                    if (initializationPacket == null) {
                        initializationPacket = CtapHidInitializationPacket.decode(buffer.array())
                        packets.add(initializationPacket)
                    } else {
                        val continuationPacket = CtapHidContinuationPacket.decode(buffer.array())
                        if (continuationPacket.channelIdentifier == initializationPacket.channelIdentifier) {
                            packets.add(continuationPacket)
                        } else {
                            Log.w(TAG, "Dropping unexpected packet: $continuationPacket")
                        }
                    }
                    if (packets.sumOf { it.data.size } >= initializationPacket.payloadLength) {
                        if (initializationPacket.channelIdentifier != channelIdentifier) {
                            packets.clear()
                            initializationPacket = null
                        } else {
                            val message = CtapHidMessage.decode(packets)
                            if (message.commandId == CtapHidKeepAliveMessage.COMMAND_ID) {
                                Log.w(TAG, "Keep alive: $message")
                                packets.clear()
                                initializationPacket = null
                            } else {
                                val response = CtapHidResponse.parse(message)
                                Log.d(TAG, "Received $response in ${packets.size} packets")
                                return@withTimeout response
                            }
                        }
                    }
                } else {
                    throw RuntimeException("Failed queuing packet")
                }
            }
            throw RuntimeException("Interrupted")
        }
    }

    override suspend fun <Q : Ctap1Request, S : Ctap1Response> runCommand(command: Ctap1Command<Q, S>): S {
        require(hasCtap1Support)
        sendRequest(CtapHidMessageRequest(command.request))
        val response = readResponse()
        if (response is CtapHidMessageResponse) {
            if (response.statusCode == 0x9000.toShort()) {
                return command.decodeResponse(response.statusCode, response.payload)
            }
            throw CtapHidMessageStatusException(response.statusCode.toInt() and 0xffff)
        }
        throw RuntimeException("Unexpected response: $response")
    }

    override suspend fun <Q: Ctap2Request, S: Ctap2Response> runCommand(command: Ctap2Command<Q, S>): S {
        require(hasCtap2Support)
        sendRequest(CtapHidCborRequest(command.request))
        val response = readResponse(command.timeout)
        if (response is CtapHidCborResponse) {
            if (response.statusCode == 0x00.toByte()) {
                return command.decodeResponse(response.payload)
            }
            throw CtapHidMessageStatusException(response.statusCode.toInt() and 0xff)
        }
        throw RuntimeException("Unexpected response: $response")
    }

    suspend fun <R> open(block: suspend (CtapHidConnection) -> R): R {
        if (!open()) throw RuntimeException("Could not open device")
        var exception: Throwable? = null
        try {
            return block(this)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            when (exception) {
                null -> close()
                else -> try {
                    close()
                } catch (closeException: Throwable) {
                    // cause.addSuppressed(closeException) // ignored here
                }
            }
        }
    }

    companion object {
        const val TAG = "FidoCtapHidConnection"
    }
}

class CtapHidMessageStatusException(val status: Int) : Exception("Received status ${status.toString(16)}")
