package com.google.android.phonesky.header

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.util.Base64
import androidx.collection.arrayMapOf
import com.android.vending.AndroidVersionMeta
import com.android.vending.DeviceMeta
import com.android.vending.EncodedTriple
import com.android.vending.EncodedTripleWrapper
import com.android.vending.IntWrapper
import com.android.vending.LicenseRequestHeader
import com.android.vending.Locality
import com.android.vending.LocalityWrapper
import com.android.vending.RequestLanguagePackage
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
import okio.ByteString
import org.microg.gms.profile.Build
import org.microg.gms.settings.SettingsContract
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.GZIPOutputStream


class GoogleApiRequest(
    var url: String,
    var method: String,
    private val user: Account,
    var context: Context,
    private val externalxpsrh: RequestLanguagePackage?
) {
    var content: ByteArray? = null
    var timeout: Int = 3000
    var headerMap: MutableMap<String, String> = arrayMapOf()
    private val tokenType = "oauth2:https://www.googleapis.com/auth/googleplay"
    var gzip: Boolean = false


    init {
        headerMap["User-Agent"] = buildUserAgent()
    }

    @SuppressLint("DefaultLocale")
    private fun buildUserAgent(): String {
        val versionName = "41.2.21-31"
        val versionCode = "84122130"
        val apiLevel = Build.VERSION.SDK_INT
        val device = Build.DEVICE
        val hardware = Build.HARDWARE
        val product = Build.PRODUCT
        val release = Build.VERSION.RELEASE
        val model = Build.MODEL
        val buildId = Build.ID
        var supportedAbis: String? = null
        supportedAbis =
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                java.lang.String.join(";", *Build.SUPPORTED_ABIS)
            } else {
                Build.CPU_ABI + ";" + Build.CPU_ABI2
            }

        return String.format(
            "Android-Finsky/%s [0] [PR] 636997666 (api=%d,versionCode=%s,sdk=%d,device=%s,hardware=%s,product=%s,platformVersionRelease=%s,model=%s,buildId=%s,isWideScreen=%d,supportedAbis=%s)",
            versionName,apiLevel,versionCode,apiLevel,
            device,
            hardware,
            product,
            release,
            model,
            buildId,
            0,
            supportedAbis
        )
    }

    private fun makeTimestamp(millis: Long): Timestamp? {
        return Timestamp.Builder()
            .seconds(millis / 1000)
            .nanos(Math.floorMod(millis, 1000) * 1000000)
            .build()
    }

    private fun getXHeaders(): String {
        val FINSKY_VERSION = "Finsky/37.5.24-29%20%5B0%5D%20%5BPR%5D%20565477504";
        var millis = System.currentTimeMillis()
        val timestamp = TimestampContainer.Builder()
            .container2(
                TimestampContainer2.Builder()
                    .wrapper(
                        TimestampWrapper.Builder()
                            .timestamp(makeTimestamp(millis)).build()
                    )
                    .timestamp(makeTimestamp(millis))
                    .build()
            )

        val androidId = SettingsContract.getSettings(
            context,
            SettingsContract.CheckIn.getContentUri(context),
            arrayOf(SettingsContract.CheckIn.ANDROID_ID)
        ) { cursor: Cursor -> cursor.getLong(0) }

        millis = System.currentTimeMillis()
        timestamp
            .container1Wrapper(
                TimestampContainer1Wrapper.Builder()
                    .androidId(androidId.toString())
                    .container(
                        TimestampContainer1.Builder()
                            .timestamp(millis.toString() + "000")
                            .wrapper(makeTimestamp(millis))
                            .build()
                    )
                    .build()
            )
        val encodedTimestamps = String(
            Base64.encode(
                Util.encodeGzip(timestamp.build().encode()),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        )
        val locality = Locality.Builder()
            .unknown1(1)
            .unknown2(2)
            .countryCode("")
            .region(
                TimestampStringWrapper.Builder()
                    .string("")
                    .timestamp(makeTimestamp(System.currentTimeMillis())).build()
            )
            .country(
                TimestampStringWrapper.Builder()
                    .string("")
                    .timestamp(makeTimestamp(System.currentTimeMillis())).build()
            )
            .unknown3(0)
            .build()
        val encodedLocality = String(
            Base64.encode(locality.encode(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
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
            .languages(externalxpsrh)
            .deviceMeta(
                DeviceMeta.Builder()
                    .android(
                        AndroidVersionMeta.Builder()
                            .androidSdk(org.microg.gms.profile.Build.VERSION.SDK_INT)
                            .buildNumber(org.microg.gms.profile.Build.ID)
                            .androidVersion(org.microg.gms.profile.Build.VERSION.RELEASE)
                            .unknown(0)
                            .build()
                    )
                    .unknown1(UnknownByte12.Builder().bytes(ByteString.EMPTY).build())
                    .unknown2(1)
                    .build()
            )
            .userAgent(
                UserAgent.Builder()
                    .deviceName(org.microg.gms.profile.Build.DEVICE)
                    .deviceHardware(org.microg.gms.profile.Build.HARDWARE)
                    .deviceModelName(org.microg.gms.profile.Build.MODEL)
                    .finskyVersion(FINSKY_VERSION)
                    .deviceProductName(org.microg.gms.profile.Build.MODEL)
                    .androidId(androidId) // must not be 0
                    .buildFingerprint(org.microg.gms.profile.Build.FINGERPRINT)
                    .build()
            )
            .uuid(
                Uuid.Builder()
                    .uuid(UUID.randomUUID().toString())
                    .unknown(2)
                    .build()
            )
            .build().encode()
        return String(Base64.encode(Util.encodeGzip(header), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
    }

    private fun getHeaders(): Map<String, String> {
        headerMap["X-PS-RH"] = getXHeaders()
        headerMap["Authorization"] = "Bearer " + AccountManager.get(context).getAuthToken(
            user, tokenType, null, false, null, null
        ).result.getString(AccountManager.KEY_AUTHTOKEN)
        return this.headerMap
    }

    fun sendRequest(externalHeader: Map<String, String>?): GoogleApiResponse? {
        val requestUrl = URL(this.url)
        val httpURLConnection = requestUrl.openConnection() as HttpURLConnection
        httpURLConnection.instanceFollowRedirects = HttpURLConnection.getFollowRedirects()
        httpURLConnection.connectTimeout = timeout
        httpURLConnection.readTimeout = timeout
        httpURLConnection.useCaches = false
        httpURLConnection.doInput = true

        val headers: MutableMap<String, String> = HashMap(
            this.getHeaders()
        )
        if (externalHeader != null) headers.putAll(externalHeader)
        for (key in headers.keys) {
            httpURLConnection.setRequestProperty(key, headers[key])
        }
        httpURLConnection.requestMethod = method
        if (this.method == "POST") {
            val content = this.content
            if (content != null) {
                httpURLConnection.doInput = true
                if (!httpURLConnection.requestProperties.containsKey("Content-Type")) {
                    httpURLConnection.setRequestProperty(
                        "Content-Type",
                        "application/x-protobuf"
                    )
                }
                val dataOutputStream: OutputStream = if (this.gzip) {
                    GZIPOutputStream(DataOutputStream(httpURLConnection.outputStream))
                } else {
                    DataOutputStream(httpURLConnection.outputStream)
                }

                dataOutputStream.write(content)
                dataOutputStream.close()
            }
        }
        val responseCode = httpURLConnection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val data = toByteArray(httpURLConnection.inputStream)
            return GoogleApiResponse.ADAPTER.decode(data)
        }

        return null
    }

    private fun toByteArray(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(1024)

        while ((inputStream.read(data, 0, data.size).also { nRead = it }) != -1) {
            buffer.write(data, 0, nRead)
        }
        buffer.flush()
        return buffer.toByteArray()
    }
}