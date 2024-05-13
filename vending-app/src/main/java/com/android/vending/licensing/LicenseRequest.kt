package com.android.vending.licensing

import android.util.Base64
import android.util.Log
import com.android.vending.AndroidVersionMeta
import com.android.vending.DeviceMeta
import com.android.vending.EncodedTriple
import com.android.vending.EncodedTripleWrapper
import com.android.vending.IntWrapper
import com.android.vending.LicenseRequestHeader
import com.android.vending.LicenseResult
import com.android.vending.Locality
import com.android.vending.LocalityWrapper
import com.android.vending.StringWrapper
import com.android.vending.Timestamp
import com.android.vending.TimestampContainer
import com.android.vending.TimestampContainer1
import com.android.vending.TimestampContainer1Wrapper
import com.android.vending.TimestampContainer2
import com.android.vending.TimestampStringWrapper
import com.android.vending.TimestampWrapper
import com.android.vending.UnknownByte12
import com.android.vending.UserAgent
import com.android.vending.Util
import com.android.vending.Uuid
import com.android.vending.V1Container
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.android.gms.common.BuildConfig
import okio.ByteString
import org.microg.gms.profile.Build
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID

abstract class LicenseRequest<T> protected constructor(
    url: String,
    private val auth: String?,
    private val successListener: Response.Listener<T>,
    errorListener: Response.ErrorListener?
) : Request<T>(
    Method.GET, url, errorListener
) {
    var ANDROID_ID: Long = 1


    override fun getHeaders(): Map<String, String> {
        var millis = System.currentTimeMillis()
        val timestamp = TimestampContainer.Builder()
            .container2(
                TimestampContainer2.Builder()
                    .wrapper(TimestampWrapper.Builder().timestamp(makeTimestamp(millis)).build())
                    .timestamp(makeTimestamp(millis))
                    .build()
            )
        millis = System.currentTimeMillis()
        timestamp
            .container1Wrapper(
                TimestampContainer1Wrapper.Builder()
                    .androidId(ANDROID_ID.toString())
                    .container(
                        TimestampContainer1.Builder()
                            .timestamp(millis.toString() + "000")
                            .wrapper(makeTimestamp(millis))
                            .build()
                    )
                    .build()
            )
        val encodedTimestamps = String(
            Base64.encode(Util.encodeGzip(timestamp.build().encode()), BASE64_FLAGS)
        )

        val locality = Locality.Builder()
            .unknown1(1)
            .unknown2(2)
            .countryCode("")
            .region(
                TimestampStringWrapper.Builder()
                    .string("").timestamp(makeTimestamp(System.currentTimeMillis())).build()
            )
            .country(
                TimestampStringWrapper.Builder()
                    .string("").timestamp(makeTimestamp(System.currentTimeMillis())).build()
            )
            .unknown3(0)
            .build()
        val encodedLocality = String(
            Base64.encode(locality.encode(), BASE64_FLAGS)
        )

        val header = LicenseRequestHeader.Builder()
            .encodedTimestamps(StringWrapper.Builder().string(encodedTimestamps).build())
            .triple(
                EncodedTripleWrapper.Builder().triple(
                    EncodedTriple.Builder()
                        .encoded1("")
                        .encoded2("")
                        .empty("")
                        .build()
                ).build()
            )
            .locality(LocalityWrapper.Builder().encodedLocalityProto(encodedLocality).build())
            .unknown(IntWrapper.Builder().integer(5).build())
            .empty("")
            .deviceMeta(
                DeviceMeta.Builder()
                    .android(
                        AndroidVersionMeta.Builder()
                            .androidSdk(Build.VERSION.SDK_INT)
                            .buildNumber(Build.ID)
                            .androidVersion(Build.VERSION.RELEASE)
                            .unknown(0)
                            .build()
                    )
                    .unknown1(
                        UnknownByte12.Builder().bytes(ByteString.EMPTY).build()
                    )
                    .unknown2(1)
                    .build()
            )
            .userAgent(
                UserAgent.Builder()
                    .deviceName(Build.DEVICE)
                    .deviceHardware(Build.HARDWARE)
                    .deviceModelName(Build.MODEL)
                    .finskyVersion(FINSKY_VERSION)
                    .deviceProductName(Build.MODEL)
                    .androidId(ANDROID_ID) // must not be 0
                    .buildFingerprint(Build.FINGERPRINT)
                    .build()
            )
            .uuid(
                Uuid.Builder()
                    .uuid(UUID.randomUUID().toString())
                    .unknown(2)
                    .build()
            )
            .build().encode()
        val xPsRh = String(Base64.encode(Util.encodeGzip(header), BASE64_FLAGS))

        Log.v(TAG, "X-PS-RH: $xPsRh")

        val userAgent =
            "$FINSKY_VERSION (api=3,versionCode=${BuildConfig.VERSION_CODE},sdk=${Build.VERSION.SDK}," +
                    "device=${encodeString(Build.DEVICE)},hardware=${encodeString(Build.HARDWARE)}," +
                    "product=${encodeString(Build.PRODUCT)},platformVersionRelease=${encodeString(Build.VERSION.RELEASE)}," +
                    "model=${encodeString(Build.MODEL)},buildId=${encodeString(Build.ID)},isWideScreen=${0}," +
                    "supportedAbis=${Build.SUPPORTED_ABIS.joinToString(";")})"
        Log.v(TAG, "User-Agent: $userAgent")

        return mapOf(
            "X-PS-RH" to xPsRh,
            "User-Agent" to userAgent,
            "Authorization" to "Bearer $auth",
            "Accept-Language" to "en-US",
            "Connection" to "Keep-Alive"
        )
    }

    override fun deliverResponse(response: T) {
        successListener.onResponse(response)
    }

    class V1(
        packageName: String,
        auth: String?,
        versionCode: Int,
        nonce: Long,
        successListener: (V1Container) -> Unit,
        errorListener: Response.ErrorListener?
    ) : LicenseRequest<V1Container>(
        "https://play-fe.googleapis.com/fdfe/apps/checkLicense?pkgn=$packageName&vc=$versionCode&nnc=$nonce",
        auth, successListener, errorListener
    ) {
        override fun parseNetworkResponse(response: NetworkResponse): Response<V1Container?>? {
            if (response.data != null) {
                try {
                    val result = LicenseResult.ADAPTER.decode(response.data)
                    return Response.success(result.information!!.v1, null)
                } catch (e: IOException) {
                    return Response.error(VolleyError(e))
                } catch (e: NullPointerException) {
                    // A field does not exist → user has no license
                    return Response.success(null, null)
                }
            } else {
                return Response.error(VolleyError("No response was returned"))
            }
        }
    }

    class V2(
        packageName: String,
        auth: String?,
        versionCode: Int,
        successListener: Response.Listener<String>,
        errorListener: Response.ErrorListener?
    ) : LicenseRequest<String>(
        "https://play-fe.googleapis.com/fdfe/apps/checkLicenseServerFallback?pkgn=$packageName&vc=$versionCode",
        auth, successListener, errorListener
    ) {
        override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
            if (response.data != null) {
                try {
                    val result = LicenseResult.ADAPTER.decode(response.data)

                    val jwt = result.information?.v2?.license?.jwt
                    return if (jwt != null) {
                        Response.success(jwt, null)
                    } else {
                        // A field does not exist → user has no license
                        Response.success(null, null)
                    }

                } catch (e: IOException) {
                    return Response.error(VolleyError(e))
                }
            } else {
                return Response.error(VolleyError("No response was returned"))
            }
        }
    }

    companion object {
        private const val TAG = "FakeLicenseRequest"

        private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        private const val FINSKY_VERSION = "Finsky/37.5.24-29%20%5B0%5D%20%5BPR%5D%20565477504"

        private fun encodeString(s: String?): String {
            return URLEncoder.encode(s).replace("+", "%20")
        }

        private fun makeTimestamp(millis: Long): Timestamp {
            return Timestamp.Builder()
                .seconds((millis / 1000))
                .nanos(((millis % 1000) * 1000000).toInt())
                .build()
        }
    }
}
