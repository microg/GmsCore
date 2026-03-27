package org.microg.gms.constellation.core

import com.squareup.wire.GrpcClient
import okhttp3.OkHttpClient
import org.microg.gms.common.Constants
import org.microg.gms.constellation.core.proto.PhoneDeviceVerificationClient
import org.microg.gms.constellation.core.proto.PhoneNumberClient

object RpcClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder()
                .header("X-Goog-Api-Key", "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk")
                .header("X-Android-Package", Constants.GMS_PACKAGE_NAME)
                .header("X-Android-Cert", Constants.GMS_PACKAGE_SIGNATURE_SHA1.uppercase())
            chain.proceed(builder.build())
        }
        .build()

    private val grpcClient: GrpcClient = GrpcClient.Builder()
        .client(client)
        // Google's constellationserver does NOT like compressed requests
        .minMessageToCompress(Long.MAX_VALUE)
        .baseUrl("https://phonedeviceverification-pa.googleapis.com/")
        .build()

    val phoneDeviceVerificationClient: PhoneDeviceVerificationClient =
        grpcClient.create(PhoneDeviceVerificationClient::class)

    val phoneNumberClient: PhoneNumberClient =
        grpcClient.create(PhoneNumberClient::class)
}
