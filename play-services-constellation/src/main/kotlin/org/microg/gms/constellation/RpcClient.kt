package org.microg.gms.constellation

import com.squareup.wire.GrpcClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.microg.gms.common.Constants
import org.microg.gms.constellation.proto.PhoneDeviceVerificationClient
import org.microg.gms.constellation.proto.PhoneNumberClient

private class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val builder = originalRequest.newBuilder()
            .header("X-Goog-Api-Key", "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk")
            .header("X-Android-Package", Constants.GMS_PACKAGE_NAME)
            .header("X-Android-Cert", Constants.GMS_PACKAGE_SIGNATURE_SHA1.uppercase())

        return chain.proceed(builder.build())
    }
}

object RpcClient {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .build()
    }

    private val grpcClient: GrpcClient by lazy {
        GrpcClient.Builder()
            .client(client)
            // Google's constellationserver does NOT like compressed requests
            .minMessageToCompress(Long.MAX_VALUE)
            .baseUrl("https://phonedeviceverification-pa.googleapis.com/")
            .build()
    }

    val phoneDeviceVerificationClient: PhoneDeviceVerificationClient by lazy {
        grpcClient.create(PhoneDeviceVerificationClient::class)
    }

    val phoneNumberClient: PhoneNumberClient by lazy {
        grpcClient.create(PhoneNumberClient::class)
    }
}
