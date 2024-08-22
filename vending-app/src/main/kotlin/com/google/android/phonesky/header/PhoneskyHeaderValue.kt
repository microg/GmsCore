package com.google.android.phonesky.header

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.database.Cursor
import android.util.Base64
import android.util.Log
import com.android.vending.RequestLanguagePackage
import com.android.vending.licensing.AUTH_TOKEN_SCOPE
import com.android.vending.licensing.encodeGzip
import com.android.vending.licensing.getDefaultLicenseRequestHeaderBuilder
import com.android.vending.licensing.getLicenseRequestHeaders
import org.microg.gms.common.Utils
import org.microg.gms.settings.SettingsContract
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream


private const val TAG = "GoogleApiRequest"
class GoogleApiRequest(
    private var url: String,
    private var method: String,
    private val account: Account,
    private var context: Context,
    private val requestLanguagePackage: List<String>
) {
    private var content: ByteArray? = null
    private var timeout: Int = 3000
    private var gzip: Boolean = false

    private fun getHeaders(): Map<String, String> {

        val auth = AccountManager.get(context).getAuthToken(
            account, AUTH_TOKEN_SCOPE, null, false, null, null
        ).result.getString(AccountManager.KEY_AUTHTOKEN) ?: ""

        if (auth.isEmpty()) {
            Log.w(TAG, "authToken is Empty!")
        }

        val androidId = SettingsContract.getSettings(
            context,
            SettingsContract.CheckIn.getContentUri(context),
            arrayOf(SettingsContract.CheckIn.ANDROID_ID)
        ) { cursor: Cursor -> cursor.getLong(0) }

        val xPsRh = String(Base64.encode(getDefaultLicenseRequestHeaderBuilder(androidId)
            .languages(RequestLanguagePackage.Builder().language(requestLanguagePackage).build())
            .build()
            .encode()
            .encodeGzip(),Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))

        val headerMap = getLicenseRequestHeaders(auth, androidId).toMutableMap()
        headerMap["X-PS-RH"] = xPsRh
        return headerMap
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
            val data = Utils.readStreamToEnd(httpURLConnection.inputStream)
            return GoogleApiResponse.ADAPTER.decode(data)
        }

        return null
    }
}