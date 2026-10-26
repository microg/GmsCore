package org.microg.gms.constellation.core

import android.content.Context
import android.util.Log
import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.microg.gms.common.Constants
import org.microg.gms.constellation.core.proto.PhoneDeviceVerificationClient
import org.microg.gms.constellation.core.proto.PhoneNumberClient
import java.util.concurrent.TimeUnit

private const val TAG = "ConstellationRpcClient"

internal fun addConstellationHeaders(request: Request, spatulaHeader: String?): Request {
    val builder = request.newBuilder()
        .header("X-Goog-Api-Key", "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk")
        .header("X-Android-Package", Constants.GMS_PACKAGE_NAME)
        .header("X-Android-Cert", Constants.GMS_PACKAGE_SIGNATURE_SHA1.uppercase())
    if (!spatulaHeader.isNullOrBlank()) {
        builder.header("X-Goog-Spatula", spatulaHeader)
    }
    return builder.build()
}

object RpcClient {
    @Volatile
    private var spatulaHeaderProvider: SpatulaHeaderProvider? = null

    fun initialize(context: Context) {
        if (spatulaHeaderProvider != null) return
        synchronized(this) {
            if (spatulaHeaderProvider == null) {
                spatulaHeaderProvider = AppCertSpatulaHeaderProvider(context.applicationContext)
            }
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val spatulaHeader = try {
                spatulaHeaderProvider?.getSpatulaHeader(Constants.GMS_PACKAGE_NAME)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to obtain X-Goog-Spatula", e)
                null
            }
            chain.proceed(addConstellationHeaders(chain.request(), spatulaHeader))
        }
        .build()

    private val grpcClient: GrpcClient = GrpcClient.Builder()
        .client(client)
        // Google's constellationserver does NOT like compressed requests
        .minMessageToCompress(Long.MAX_VALUE)
        .baseUrl("https://phonedeviceverification-pa.googleapis.com/")
        .build()

    val phoneDeviceVerificationClient: PhoneDeviceVerificationClient =
        grpcClient.create<PhoneDeviceVerificationClient>()

    val phoneNumberClient: PhoneNumberClient =
        grpcClient.create<PhoneNumberClient>()
}
