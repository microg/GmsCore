package org.microg.gms.droidguard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.droidguard.DroidGuardRequest
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer


private const val DG_PROTOCOL_VERSION = 1
private const val DG_CMD_GET_RESULT = 1
private const val DG_CMD_GET_RESULT_MULTI = 2
private const val DG_CMD_GET_RESULT_MULTI_CONTINUE = 3

data class RemoteDroidGuardConfig(
    val host: String,
)

class DroidGuardRemote(private val config: RemoteDroidGuardConfig) {
    suspend fun getResult(context: Context, request: DroidGuardRequest): DroidGuardResult = withContext(Dispatchers.IO) {
        Socket(config.host, config.port).use { socket ->
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())
            // Write protocol version
            output.writeInt(DG_PROTOCOL_VERSION)
            
            // Check if this is a multi-step request
            val isMultiStep = request.javaClass.getDeclaredFields().any { it.name == "flow" || it.name == "session" }
            
            if (isMultiStep) {
                // Use multi-step protocol
                output.writeInt(DG_CMD_GET_RESULT_MULTI)
                writeRequest(output, request)
                output.flush()
                
                // Handle multi-step flow
                var step = 0
                while (true) {
                    val responseType = input.readInt()
                    when (responseType) {
                        1 -> { // Need more data
                            val extraDataLength = input.readInt()
                            val extraData = ByteArray(extraDataLength)
                            input.readFully(extraData)
                            
                            // Process the extra data request locally if needed
                            // This is where the multi-step flow continues
                            val localResult = processLocalStep(context, request, extraData, step)
                            
                            // Send continuation
                            output.writeInt(DG_CMD_GET_RESULT_MULTI_CONTINUE)
                            output.writeInt(localResult.size)
                            output.write(localResult)
                            output.flush()
                            step++
                        }
                        2 -> { // Final result
                            val resultLength = input.readInt()
                            val result = ByteArray(resultLength)
                            input.readFully(result)
                            return@withContext DroidGuardResult(result)
                        }
                        3 -> { // Error
                            val errorLength = input.readInt()
                            val errorBytes = ByteArray(errorLength)
                            input.readFully(errorBytes)
                            throw IOException(String(errorBytes))
                        }
                        else -> throw IOException("Unknown response type: $responseType")
                    }
                }
            } else {
                // Single step (original behavior)
                output.writeInt(DG_CMD_GET_RESULT)
                writeRequest(output, request)
                output.flush()
                
                // Read response
                val resultLength = input.readInt()
                val result = ByteArray(resultLength)
                input.readFully(result)
                return@withContext DroidGuardResult(result)
            }
        }
    }
    
    private fun processLocalStep(context: Context, request: DroidGuardRequest, extraData: ByteArray, step: Int): ByteArray {
        // For Play Integrity, we may need to do local processing between steps
        // This allows the remote server to request additional attestation data
        // The default implementation just returns the extra data as-is
        // Subclasses or future versions can override this behavior
        return try {
            // Try to extract any local state needed for the multi-step flow
            // This is a placeholder for any local processing that might be needed
            extraData
        } catch (e: Exception) {
            Log.w(TAG, "Error in local step processing", e)
            extraData
        }
    }
    
        output.writeInt(request.packageName?.length ?: 0)
        output.write(request.packageName?.toByteArray() ?: ByteArray(0))
        
        val requestBytes = request.toByteArray()
        output.writeInt(requestBytes.size)
        output.write(requestBytes)