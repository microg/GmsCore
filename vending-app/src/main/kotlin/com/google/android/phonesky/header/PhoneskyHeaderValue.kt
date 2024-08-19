package com.google.android.phonesky.header

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.collection.arrayMapOf
import com.android.vending.ExperimentAndConfigs.readExperimentsFlag
import com.android.vending.ExperimentAndConfigs.toByteArray
import com.google.android.phonesky.header.PayloadsProtoStore.readCache
import okio.ByteString.Companion.encodeUtf8
import org.microg.vending.billing.CheckinServiceClient.getConsistencyToken
import org.microg.vending.billing.GServices.getString
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

object PhoneskyHeaderValue {
    var TAG: String = PhoneskyHeaderValue::class.java.simpleName
    private const val PHONESKY_HEADER_FILE = "finsky/shared/phonesky_header_valuestore.pb"

    @SuppressLint("HardwareIds")
    fun init(applicationContext: Context): PhoneskyValueStore? {
        try {
            val initiated = PhoneskyValue.Builder()

            initiated.ConsistencyTokenWrapperValue(
                ConsistencyTokenWrapper.Builder()
                    .ConsistencyToken(getConsistencyToken(applicationContext))
                    .unknowTokenf("").build()
            )

            initiated.baseDeviceInfoValue(
                BaseDeviceInfo.Builder()
                    .device(Build.DEVICE)
                    .hardware(Build.HARDWARE)
                    .model(Build.MODEL)
                    .product(Build.PRODUCT)
                    .androidId(
                        getString(applicationContext.contentResolver, "android_id", "")!!
                            .toLong()
                    )
                    .gpVersion(
                        Uri.encode(
                            applicationContext.packageManager.getApplicationInfo(
                                applicationContext.packageName,
                                PackageManager.GET_META_DATA
                            ).metaData.getString("GpVersion")
                        ).replace("(", "%28").replace(")", "%29")
                    )
                    .fingerPrint(Build.FINGERPRINT).build()
            )

            initiated.deviceBuildInfoValue(
                DeviceBuildInfo.Builder()
                    .buildInfo(
                        BuildInfo.Builder()
                            .sdkInt(Build.VERSION.SDK_INT)
                            .id(Build.ID)
                            .release(Build.VERSION.RELEASE)
                            .constInte(84122130).build()
                    )
                    .marketClientId("am-google") //"market_client_id"
                    .unknowBooleD(true) //getResources(xxx)^1
                    .build()
            )

            val result = PhoneskyValueStore.Builder()
            val phoneskyValueMutableMap = result.values.toMutableMap()
            phoneskyValueMutableMap["<device>"] = initiated.build()
            result.values = phoneskyValueMutableMap
            return result.build()
        } catch (e: Exception) {
            Log.w(TAG, "PhoneskyHeaderValue.Init", e)
        }
        return null
    }

    fun getPhoneskyHeader(context: Context, account: Account) {
        var request = GoogleApiRequest(
            "https://play-fe.googleapis.com/fdfe/toc?nocache_isui=true",
            "GET",
            account,
            context,
            buildataFdfe(context)
        )
        val result = request.sendRequest(null)
        val tocToken = TocToken.Builder()
            .token(result!!.fdfeApiResponseValue?.tocApi?.tocTokenValue?.encodeUtf8())
            .build()
        writePhonesky(context, "<device>", object : WritePhoneskyCallback {
            override fun modify(data: PhoneskyValue.Builder): PhoneskyValue {
                return data.tocTokenValue(tocToken).build()
            }

        })

        val firstSyncData = SyncReqWrapper.Builder().mvalue(
            listOf(
                SyncRequest.Builder()
                    .UnknowTypeFirstSyncValue(UnknowTypeFirstSync.Builder().build()).build()
            )
        ).build()
        request = GoogleApiRequest(
            "https://play-fe.googleapis.com/fdfe/sync",
            "POST",
            account,
            context,
            buildataFdfe(context)
        )
        request.content = firstSyncData.encode()
        val resultSyncFirst = request.sendRequest(null)

        writePhonesky(context, "<device>", object : WritePhoneskyCallback {
            override fun modify(data: PhoneskyValue.Builder): PhoneskyValue {
                return data.sysncTokenValue(
                    resultSyncFirst!!.fdfeApiResponseValue?.syncResult?.syncTokenValue
                ).build()
            }
        })

        val requestData = readCache(context)
        request = GoogleApiRequest(
            "https://play-fe.googleapis.com/fdfe/sync?nocache_qos=lt",
            "POST",
            account,
            context,
            buildataFdfe(context)
        )
        request.content = requestData!!.encode()
        val resultSync = request.sendRequest(null)

        writePhonesky(context, "<device>", object : WritePhoneskyCallback {
            override fun modify(data: PhoneskyValue.Builder): PhoneskyValue {
                return data.sysncTokenValue(
                    resultSync!!.fdfeApiResponseValue?.syncResult?.syncTokenValue
                ).build()
            }
        })

        writePhonesky(context, account.name, object : WritePhoneskyCallback {
            override fun modify(data: PhoneskyValue.Builder): PhoneskyValue {
                return data.experimentWrapperValue(
                    ExperimentWrapper.Builder().experServerTokenValue(
                        getExperimentTokenFor(context, account)
                    ).build()
                ).build()
            }
        })

        writePhonesky(context, "", object : WritePhoneskyCallback {
            override fun modify(data: PhoneskyValue.Builder): PhoneskyValue {
                return data.experimentWrapperValue(
                    ExperimentWrapper.Builder().experServerTokenValue(
                        getExperimentTokenFor(context, null)
                    ).build()
                ).build()
            }
        })
    }

    private fun getExperimentTokenFor(context: Context, account: Account?): ExperServerToken {
        val dataRegular = readExperimentsFlag(
            context,
            "com.google.android.finsky.regular",
            if (account == null) "" else account.name
        )
        val dataStable = readExperimentsFlag(context, "com.google.android.finsky.stable", "")
        val result = ExperServerToken.Builder()
        if (dataRegular != null && !TextUtils.isEmpty(dataRegular.serverToken)) {
            result.regularServerToken(dataRegular.serverToken)
        }
        if (dataStable != null && !TextUtils.isEmpty(dataStable.serverToken)) {
            result.stableServerToken(dataStable.serverToken)
        }
        return result.build()
    }

    //build base X-PS-RH for /fdfe/*
    fun buildataFdfe(context: Context): PhoneskyValue {
        return PhoneskyValue.Builder()
            .unknowFieldk(PhoneskyUnknowFieldK.Builder().mvalue(5).build())
            .baseDeviceInfoValue(
                BaseDeviceInfo.Builder().androidId(
                    getString(context.contentResolver, "android_id", "")!!
                        .toLong()
                ).build()
            )
            .unknowDeviceIdValue(
                UnknowDeviceId.Builder().uuid("00000000-0000-0000-0000-000000000000").type(1)
                    .build()
            ).build()
    }

    private fun writePhonesky(context: Context, key: String, callback: WritePhoneskyCallback) {
        val file = File(context.filesDir, PHONESKY_HEADER_FILE)
        var existData: PhoneskyValueStore.Builder? = null
        if (file.exists()) {
            val input = FileInputStream(file)
            existData = PhoneskyValueStore.ADAPTER.decode(input).newBuilder()
            input.close()
        } else {
            if (file.parentFile?.exists() == true || file.parentFile?.mkdirs() == true) {
                if (file.createNewFile()) {
                    existData = init(context)?.newBuilder()
                } else {
                    throw RuntimeException("create file failed")
                }
            }
        }
        if (existData != null) {
            val phoneskyValueMap = existData.values.toMutableMap()
            if (existData.values.containsKey(key)) {

                val modifed = callback.modify(if (existData.values[key] != null) {
                    existData.values[key]!!.newBuilder()
                } else {
                    PhoneskyValue.Builder()
                })
                phoneskyValueMap[key] = modifed
            } else {
                val modifed = callback.modify(PhoneskyValue.Builder())
                phoneskyValueMap[key] = modifed
            }
            existData.values = phoneskyValueMap
            val outputStream = FileOutputStream(file)
            outputStream.write(existData.build().encode())
            outputStream.close()
        }
    }

    interface WritePhoneskyCallback {
        fun modify(data: PhoneskyValue.Builder): PhoneskyValue
    }

    class GoogleApiRequest(
        var url: String,
        var method: String,
        private val user: Account,
        var context: Context,
        private val externalxpsrh: PhoneskyValue?
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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

        fun addHeader(key: String, value: String) {
            headerMap[key] = value
        }

        fun getHeaders(): Map<String, String> {
            val phoneksyHeaderFile = File(context.filesDir, PHONESKY_HEADER_FILE)
            var existData = PhoneskyValueStore.Builder().build()
            if (phoneksyHeaderFile.exists()) {
                val input = FileInputStream(phoneksyHeaderFile)
                existData = PhoneskyValueStore.ADAPTER.decode(input)
                input.close()
            }
            var xpsrh = PhoneskyValue.Builder().build()
            if (existData.values.containsKey("<device>")) {
                xpsrh = existData.values["<device>"]!!
            }
            if (existData.values.containsKey(user.name)) {
                mergeProto(xpsrh, existData.values[user.name])
            }
            if (externalxpsrh != null) {
                mergeProto(xpsrh, externalxpsrh)
            }
            headerMap["X-PS-RH"] = Base64.encodeToString(
                gzip(
                    xpsrh!!.encode()
                ), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            headerMap["Authorization"] = "Bearer " + AccountManager.get(context).getAuthToken(
                user, tokenType, null, false, null, null
            ).result.getString(AccountManager.KEY_AUTHTOKEN)
            return this.headerMap
        }

        fun mergeProto(data1: PhoneskyValue?, data2: PhoneskyValue?) {
            for (data in PhoneskyValue::class.java.declaredFields) {
                data.isAccessible = true
                if (data[data2] != null && data[data1] == null) {
                    data[data1] = data[data2]
                }
            }
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

        companion object {
            fun gzip(arr_b: ByteArray?): ByteArray {
                try {
                    ByteArrayOutputStream().use { byteArrayOutputStream ->
                        GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                            gzipOutputStream.write(arr_b)
                            gzipOutputStream.finish()
                            val arr_b1 = byteArrayOutputStream.toByteArray()
                            arr_b1[9] = 0
                            return arr_b1
                        }
                    }
                } catch (iOException0: IOException) {
                    Log.w("Unexpected %s", arrayOf<Any>(iOException0).contentToString())
                    return ByteArray(0)
                }
            }
        }
    }
}
