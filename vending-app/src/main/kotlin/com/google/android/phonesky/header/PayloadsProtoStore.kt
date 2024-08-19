/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.phonesky.header

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.opengl.GLES10
import android.os.Build
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import org.microg.vending.billing.GServices.getString
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Objects
import java.util.Random
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

object PayloadsProtoStore {
    private val TAG: String = PayloadsProtoStore::class.java.simpleName
    private const val FILE_NAME = "finsky/shared/payload_valuestore.pb"

    fun accountSha256(account: Account, context: Context): String? {
        try {
            val androidId = getString(context.contentResolver, "android_id", "")
            val androidIdAcc = (androidId + "-" + account.name).toByteArray()
            val messageDigest0 = MessageDigest.getInstance("SHA256")
            messageDigest0.update(androidIdAcc, 0, androidIdAcc.size)
            return Base64.encodeToString(messageDigest0.digest(), 11)
        } catch (ignored: Exception) {
            return null
        }
    }

    @JvmStatic
    fun readCache(context: Context): SyncReqWrapper? {
        Log.d(TAG, "readCache: ")
        val cacheFile = File(context.filesDir, FILE_NAME)
        if (!cacheFile.exists()) {
            return null
        }
        try {
            FileInputStream(cacheFile).use { inputStream ->
                return SyncReqWrapper.ADAPTER.decode(inputStream)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Error reading person from file", e)
            return null
        }
    }

    fun cachePayload(account: Account, context: Context) {
        Log.d(TAG, "cachePayload: ")
        val builder = SyncReqWrapper.Builder()
        val payloads = buildPayloads(account, context)
        for (payload in payloads) {
            if (payload != null) {
                builder.mvalue = builder.mvalue.toMutableList().apply { add(payload) }
            }
        }
        val cacheFile = File(context.filesDir, FILE_NAME)
        try {
            if (!cacheFile.exists()) {
                if (!cacheFile.parentFile.exists()) cacheFile.parentFile.mkdirs()
                cacheFile.createNewFile()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Create payload_valuestore.pb failed !")
            return
        }
        try {
            FileOutputStream(cacheFile).use { outputStream ->
                outputStream.write(builder.build().encode())
                Log.d(TAG, "Person written to file: " + cacheFile.absolutePath)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Error writing person to file", e)
        }
    }

    private fun generateRandomIMEI(): String {
        val random = Random()

        // Generate the first 14 random digits
        val imeiBuilder = StringBuilder()
        for (i in 0..13) {
            val digit = random.nextInt(10)
            imeiBuilder.append(digit)
        }

        // Calculate the check digit
        val imei14 = imeiBuilder.toString()
        val checkDigit = calculateLuhnCheckDigit(imei14)

        // Splice into a complete IMEI
        imeiBuilder.append(checkDigit)
        return imeiBuilder.toString()
    }

    private fun calculateLuhnCheckDigit(imei14: String): Int {
        var sum = 0
        for (i in 0 until imei14.length) {
            var digit = Character.getNumericValue(imei14[i])
            if (i % 2 == 1) {
                digit *= 2
            }
            if (digit > 9) {
                digit -= 9
            }
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    private fun buildPayloads(account: Account, context: Context): Array<SyncRequest?> {
        val gpuInfos: ArrayList<FetchedGlStrings?> = fetchGLInfo() ?: return arrayOfNulls(0)
        //---------------------------------------GPU info--------------------------------------------------------------------
        val accountSha256 = accountSha256(account, context)
        val accountAssValue = AccountAssValue.Builder().mvalue(accountSha256).build()
        val accountAossiationPayload =
            AccountAossiationPayload.Builder().mvalue(accountAssValue).build()
        val accountAossiationPayloadRequest =
            SyncRequest.Builder().AccountAossiationPayloadVALUE(accountAossiationPayload).build()
        //--------------------------------------------------------------------------------------------------------------------
        val carrierPropertiesPayloadRequest = createCarrierPropertiesPayloadRequest(context)

        val deviceAccountsPayloadRequest = createDeviceAccountsPayloadRequest(context)

        val deviceInfoCollect = createDeviceInfoCollect(context, gpuInfos.filterNotNull())

        val deviceCapabilitiesPayloadRequest =
            createDeviceCapabilitiesPayloadRequest(deviceInfoCollect)

        val deviceInputPropertiesPayloadRequest =
            createDeviceInputPropertiesPayloadRequest(deviceInfoCollect)

        val deviceModelPayloadRequest = createDeviceModelPayloadRequest()

        val enterprisePropertiesPayloadRequest = createEnterprisePropertiesPayloadRequest(context)

        val hardwareIdentifierPayloadRequest = createHardwareIdentifierPayloadRequest(context)

        val hardwarePropertiesPayloadRequest =
            createHardwarePropertiesPayloadRequest(deviceInfoCollect)

        val localePropertiesPayloadRequest = createLocalePropertiesPayloadRequest()

        val playPartnerPropertiesPayloadRequest = createPlayPartnerPropertiesPayloadRequest()

        val playPropertiesPayloadRequest = createPlayPropertiesPayload(context)

        val screenPropertiesPayloadRequest = createScreenPropertiesPayloadRequest(deviceInfoCollect)

        val systemPropertiesPayloadRequest = createSystemPropertiesPayloadRequest(deviceInfoCollect)

        val gpuPayloadRequest = createGpuPayloadRequest(gpuInfos.filterNotNull())

        return arrayOf(
            accountAossiationPayloadRequest,
            carrierPropertiesPayloadRequest,
            deviceAccountsPayloadRequest,
            deviceCapabilitiesPayloadRequest,
            deviceInputPropertiesPayloadRequest,
            deviceModelPayloadRequest,
            enterprisePropertiesPayloadRequest,
            hardwareIdentifierPayloadRequest,
            hardwarePropertiesPayloadRequest,
            localePropertiesPayloadRequest,  //                NOTIFICATION_ROUTING_INFO_PAYLOAD,
            playPartnerPropertiesPayloadRequest,
            playPropertiesPayloadRequest,
            screenPropertiesPayloadRequest,
            systemPropertiesPayloadRequest,
            gpuPayloadRequest
        )
    }

    private fun createCarrierPropertiesPayloadRequest(context: Context): SyncRequest? {
        var carrierPropertiesPayloadRequest: SyncRequest? = null
        try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            @SuppressLint("HardwareIds") val subscriberId1 =
                (telephonyManager.subscriberId.toLong() / 100000L).toString() + "00000"
            val groupIdLevel = telephonyManager.groupIdLevel1
            val simOperator = telephonyManager.simOperator
            val operatorName = telephonyManager.simOperatorName
            var simcardId = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                simcardId = telephonyManager.simCarrierId
            }
            var carrierIdFromSimMccMnc = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                carrierIdFromSimMccMnc = telephonyManager.carrierIdFromSimMccMnc
            }

            val telephonyInfo = TelephonyInfo.Builder().subscriberId1(subscriberId1.toLong())
                .operatorName(operatorName).groupidLevel(groupIdLevel).simcardId(simcardId)
                .CarrierIdFromSimMccMnc(carrierIdFromSimMccMnc).build()

            val telephonyStateWrapper =
                TelephonyStateWrapper.Builder().mvalue(telephonyInfo).build()
            val carrierPropertiesPayload = CarrierPropertiesPayload.Builder()
                .telephonyStateValue(telephonyStateWrapper).simOperator(simOperator).build()
            carrierPropertiesPayloadRequest =
                SyncRequest.Builder().CarrierPropertiesPayloadVALUE(carrierPropertiesPayload)
                    .build()
        } catch (securityException: SecurityException) {
            Log.w(TAG, "SecurityException when reading IMSI.", securityException)
        } catch (stateException: IllegalStateException) {
            Log.w(
                TAG,
                "IllegalStateException when reading IMSI. This is a known SDK 31 Samsung bug.",
                stateException
            )
        }
        return carrierPropertiesPayloadRequest
    }

    private fun createDeviceAccountsPayloadRequest(context: Context): SyncRequest {
        val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        val accounts = accountManager.accounts

        val builder = DeviceAccountsPaylaod.Builder()
        for (account in accounts) {
            builder.mvalue = builder.mvalue.toMutableList().apply {
                add(
                    AccountAssValue.Builder().mvalue(accountSha256(account, context)).build()
                )
            }
        }
        return SyncRequest.Builder().DeviceAccountsPaylaodVALUE(builder.build()).build()
    }

    private fun createDeviceInfoCollect(
        context: Context,
        gpuInfos: List<FetchedGlStrings>
    ): DeviceInfoCollect {
        val builder = DeviceInfoCollect.Builder()
        builder.reqTouchScreen(0).reqKeyboardType(0).reqNavigation(0).desityDeviceStablePoint(0)
            .reqInputFeatures1(false)
            .reqInputFeatures2(false).desityDeviceStable(0).reqGlEsVersion(0)

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        if (configurationInfo != null) {
            if (configurationInfo.reqTouchScreen != Configuration.TOUCHSCREEN_UNDEFINED) {
                builder.reqTouchScreen(configurationInfo.reqTouchScreen)
            }
            if (configurationInfo.reqKeyboardType != Configuration.KEYBOARD_UNDEFINED) {
                builder.reqKeyboardType(configurationInfo.reqKeyboardType)
            }
            if (configurationInfo.reqNavigation != Configuration.NAVIGATION_UNDEFINED) {
                builder.reqNavigation(configurationInfo.reqNavigation)
            }
            builder.reqGlEsVersion(configurationInfo.reqGlEsVersion)
            builder.reqInputFeatures1((configurationInfo.reqInputFeatures and 1) == 1)
                .reqInputFeatures2(
                    (configurationInfo.reqInputFeatures and 2) > 0
                )
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        if (windowManager != null) {
            val display = windowManager.defaultDisplay
            display.getSize(size)

            builder.displaySizex(size.x).displaySizey(size.y)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.desityDeviceStable(DisplayMetrics.DENSITY_DEVICE_STABLE)
                .desityDeviceStablePoint(
                    calculatePoint(size, DisplayMetrics.DENSITY_DEVICE_STABLE)
                )
        }

        val configuration = context.resources.configuration
        builder.screenLayout(configuration.screenLayout)
            .smallestScreenWidthDp(configuration.smallestScreenWidthDp)
            .systemSharedLibraryNames(Arrays.asList(*Objects.requireNonNull(context.packageManager.systemSharedLibraryNames)))
            .locales(Arrays.asList(*context.assets.locales))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.glExtensions(
                gpuInfos.stream()
                    .flatMap { fetchedGlStrings: FetchedGlStrings -> Arrays.stream(fetchedGlStrings.glExtensions) }
                    .collect(Collectors.toList()))
                .isLowRamDevice(activityManager.isLowRamDevice)
        }

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        builder.totalMem(memoryInfo.totalMem)
            .availableProcessors(Runtime.getRuntime().availableProcessors())

        val systemAvailableFeatures = context.packageManager.systemAvailableFeatures
        for (featureInfo in systemAvailableFeatures) {
            if (!TextUtils.isEmpty(featureInfo.name)) {
                var featureInfoProto = FeatureInfoProto.Builder().build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    featureInfoProto = FeatureInfoProto.Builder().name(featureInfo.name)
                        .version(featureInfo.version).build()
                }
                builder.featureInfos = builder.featureInfos.toMutableList().apply {
                    add(featureInfoProto)
                }
                builder.featureNames = builder.featureNames.toMutableList().apply {
                    add(featureInfoProto.name!!)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.supportedAbis(java.util.List.of(*Build.SUPPORTED_ABIS))
        }

        var prop = getSystemProperty("ro.oem.key1", "")
        if (!TextUtils.isEmpty(prop)) {
            builder.oemkey1(prop)
        }
        builder.buildCodeName(Build.VERSION.CODENAME)
        prop = getSystemProperty("ro.build.version.preview_sdk_fingerprint", "")
        if (!TextUtils.isEmpty(prop)) {
            builder.previewSdkFingerprint(prop)
        }
        return builder.build()
    }

    private fun createDeviceCapabilitiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val builder = DeviceCapabilitiesPayload.Builder()
        builder.glExtensions(deviceInfoCollect.glExtensions)
        for (featureInfoProto in deviceInfoCollect.featureInfos) {
            builder.featureInfos = builder.featureInfos.toMutableList().apply {
                add(
                    FeatureInfoProto.Builder().name(featureInfoProto.name)
                        .version(featureInfoProto.version).build()
                )
            }
        }
        builder.systemSharedLibraryNames(deviceInfoCollect.systemSharedLibraryNames)
            .locales(deviceInfoCollect.locales).unknowFlag(false)
        return SyncRequest.Builder().DeviceCapabilitiesPayloadVALUE(builder.build()).build()
    }

    private fun createDeviceInputPropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val builder = DeviceInputPropertiesPayload.Builder()
        builder.reqInputFeatures1(deviceInfoCollect.reqInputFeatures1)
            .reqKeyboardType(deviceInfoCollect.reqKeyboardType)
            .reqNavigation(deviceInfoCollect.reqNavigation)
        return SyncRequest.Builder().DeviceInputPropertiesPayloadVALUE(builder.build()).build()
    }

    private fun createDeviceModelPayloadRequest(): SyncRequest {
        val builder = DeviceModelPayload.Builder()
        builder.MANUFACTURER(Build.MANUFACTURER).MODEL(Build.MODEL).DEVICE(Build.DEVICE).PRODUCT(
            Build.PRODUCT
        ).BRAND(Build.BRAND)
        return SyncRequest.Builder().DeviceModelPayloadVALUE(builder.build()).build()
    }

    private fun createEnterprisePropertiesPayloadRequest(context: Context): SyncRequest {
        val enterprisePropertiesPayload = EnterprisePropertiesPayload.Builder()
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val activeAdmins = devicePolicyManager.activeAdmins
        if (activeAdmins != null) {
            for (componentName in activeAdmins) {
                val packageName = componentName.packageName
                var packageInfo: PackageInfo? = null
                try {
                    packageInfo = context.packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNATURES
                    )
                } catch (ignored: Exception) {
                }

                val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
                var isProfileOwner = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    isProfileOwner = devicePolicyManager.isProfileOwnerApp(packageName)
                }

                val profileInfoTemp =
                    ProfileInfoTemp.Builder().packageName(componentName.packageName)
                        .policyTypeValue(if (isDeviceOwner) PolicyType.MANAGED_DEVICE else if (isProfileOwner) PolicyType.MANAGED_PROFILE else PolicyType.LEGACY_DEVICE_ADMIN)
                        .pkgSHA1(calculateSHA(packageInfo!!.signatures[0].toByteArray(), "SHA1"))
                        .pkgSHA256(calculateSHA(packageInfo.signatures[0].toByteArray(), "SHA256"))
                        .build()
                val profileInfo = ProfileInfo.Builder().pkgName(profileInfoTemp.packageName)
                    .pkgSHA1(profileInfoTemp.pkgSHA1).pkgSHA256(profileInfoTemp.pkgSHA256)
                    .policyTypeValue(MangedScope.fromValue(profileInfoTemp.policyTypeValue!!.value))
                    .build()
                if (isProfileOwner) {
                    enterprisePropertiesPayload.profileOwner(profileInfo)
                }
                enterprisePropertiesPayload.mdefault = enterprisePropertiesPayload.mdefault.toMutableList().apply {
                    add(profileInfo)
                }
            }
        }
        return SyncRequest.Builder()
            .enterprisePropertiesPayload(enterprisePropertiesPayload.build()).build()
    }

    private fun createHardwareIdentifierPayloadRequest(context: Context): SyncRequest {
        val builder = HardwareIdentifierPayload.Builder()
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var imeid: Long = 0
        if (telephonyManager != null) {
            //random imei
            val randomIMEI = generateRandomIMEI()
            imeid = if (TextUtils.isEmpty(randomIMEI) || !Pattern.compile("^[0-9]{15}$")
                    .matcher(randomIMEI).matches()
            ) 0L else randomIMEI.toLong(10) or 0x1000000000000000L
            if (imeid == 0L) {
                var meid = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    meid = telephonyManager.meid
                }
                if (!TextUtils.isEmpty(meid) && Pattern.compile("^[0-9a-fA-F]{14}$").matcher(meid)
                        .matches()
                ) {
                    imeid = meid.toLong(16) or 0x1100000000000000L
                    if (imeid == 0L) {
                        if (context.packageManager.checkPermission(
                                "android.permission.READ_PRIVILEGED_PHONE_STATE",
                                "com.android.vending"
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            var serial = ""
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                serial = Build.getSerial()
                            }
                            if (TextUtils.isEmpty(serial) && serial != "unknown") {
                                try {
                                    val serialShaByte = MessageDigest.getInstance("SHA1")
                                        .digest(serial.toByteArray())
                                    imeid =
                                        ((serialShaByte[0].toLong()) and 0xFFL) shl 0x30 or 0x1400000000000000L or (((serialShaByte[1].toLong()) and 0xFFL) shl 40) or (((serialShaByte[2].toLong()) and 0xFFL) shl 0x20) or (((serialShaByte[3].toLong()) and 0xFFL) shl 24) or (((serialShaByte[4].toLong()) and 0xFFL) shl 16) or (((serialShaByte[5].toLong()) and 0xFFL) shl 8) or ((serialShaByte[6].toLong()) and 0xFFL)
                                } catch (noSuchAlgorithmException0: NoSuchAlgorithmException) {
                                    Log.w(TAG, "No support for sha1?")
                                }
                            }
                        }
                    }
                }
            }
            builder.imeid(imeid)
        }
        return SyncRequest.Builder().HardwareIdentifierPayloadVALUE(builder.build()).build()
    }

    private fun createHardwarePropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val HardwarePropertiesPayload_ = HardwarePropertiesPayload.Builder()
        HardwarePropertiesPayload_.isLowRamDevice(deviceInfoCollect.isLowRamDevice)
            .totalMem(deviceInfoCollect.totalMem)
            .availableProcessors(deviceInfoCollect.availableProcessors)
            .supportedAbis(deviceInfoCollect.supportedAbis).build()
        return SyncRequest.Builder()
            .HardwarePropertiesPayloadVALUE(HardwarePropertiesPayload_.build()).build()
    }

    private fun createLocalePropertiesPayloadRequest(): SyncRequest {
        val builder = LocalePropertiesPayload.Builder().b("GMT+08:00")
        return SyncRequest.Builder().LocalePropertiesPayloadVALUE(builder.build()).build()
    }

    private fun createPlayPartnerPropertiesPayloadRequest(): SyncRequest {
        val builder = PlayPartnerPropertiesPayload.Builder()
        builder.marketId("am-google").partnerIdMs("play-ms-android-google")
            .partnerIdAd("play-ad-ms-android-google")
        return SyncRequest.Builder().PlayPartnerPropertiesPayloadVALUE(builder.build()).build()
    }

    private fun createPlayPropertiesPayload(context: Context): SyncRequest {
        var version = 0
        try {
            version = context.packageManager.getPackageInfo("com.android.vending", 0).versionCode
        } catch (`packageManager$NameNotFoundException0`: PackageManager.NameNotFoundException) {
            Log.w(TAG, "[DAS] Could not find our package", `packageManager$NameNotFoundException0`)
        }
        val playPropertiesPayload = PlayPropertiesPayload.Builder().playVersion(version).build()
        return SyncRequest.Builder().PlayPropertiesPayloadVALUE(playPropertiesPayload).build()
    }

    private fun createScreenPropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val builder = ScreenPropertiesPayload.Builder()
        builder.reqTouchScreen(deviceInfoCollect.reqTouchScreen)
            .displaySizex(deviceInfoCollect.displaySizex)
            .displaySizey(deviceInfoCollect.displaySizey)
            .desityDeviceStablePoint(deviceInfoCollect.desityDeviceStablePoint)
            .desityDeviceStable(deviceInfoCollect.desityDeviceStable)
        return SyncRequest.Builder().ScreenPropertiesPayloadVALUE(builder.build()).build()
    }

    private fun createSystemPropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val SystemPropertiesPayload_ = SystemPropertiesPayload.Builder()
        SystemPropertiesPayload_.fingerprint("google/sunfish/sunfish:13/TQ2A.230405.003/9719927:user/release-keys")
            .sdkInt(Build.VERSION.SDK_INT.toLong())
            .previewSdkFingerprint(deviceInfoCollect.previewSdkFingerprint)
            .buildCodeName(deviceInfoCollect.buildCodeName).oemkey1(deviceInfoCollect.oemkey1)
            .reqGlEsVersion(deviceInfoCollect.reqGlEsVersion)
        return SyncRequest.Builder().SystemPropertiesPayloadVALUE(SystemPropertiesPayload_.build())
            .build()
    }

    private fun createGpuPayloadRequest(gpuInfos: List<FetchedGlStrings>): SyncRequest {
        var gpuInfos = gpuInfos
        var gpuPayloads = emptyList<GpuPayload>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gpuInfos = gpuInfos.stream()
                .filter { fetchedGlStrings: FetchedGlStrings -> !fetchedGlStrings.glRenderer!!.isEmpty() || !fetchedGlStrings.glVendor!!.isEmpty() || !fetchedGlStrings.glVersion!!.isEmpty() }
                .collect(Collectors.toList())
            val maxVersion = gpuInfos.stream()
                .max(Comparator.comparingInt { fetchedGlStrings: FetchedGlStrings -> fetchedGlStrings.contextClientVersion })
                .map { obj: FetchedGlStrings -> obj.contextClientVersion }
            if (maxVersion.isPresent) {
                gpuInfos = gpuInfos.stream()
                    .filter { fetchedGlStrings: FetchedGlStrings -> fetchedGlStrings.contextClientVersion == maxVersion.get() }
                    .collect(Collectors.toList())
            }
            gpuPayloads = gpuInfos.stream().map { fetchedGlStrings: FetchedGlStrings ->
                val gpuInfoWrapper_ = GpuInfoWrapper.Builder()
                if (!TextUtils.isEmpty(fetchedGlStrings.glRenderer)) gpuInfoWrapper_.glRenderer(
                    fetchedGlStrings.glRenderer
                )
                if (!TextUtils.isEmpty(fetchedGlStrings.glVendor)) gpuInfoWrapper_.glVendor(
                    fetchedGlStrings.glVendor
                )
                if (!TextUtils.isEmpty(fetchedGlStrings.glVersion)) gpuInfoWrapper_.glVersion(
                    fetchedGlStrings.glVersion
                )
                GpuPayload.Builder().gpuInfo(gpuInfoWrapper_.build()).build()
            }.distinct().collect(Collectors.toList())
        }

        return SyncRequest.Builder().GpuPayloadVALUE(
            if (gpuPayloads.isEmpty()) GpuPayload.Builder().build() else gpuPayloads[0]
        ).build()
    }

    private fun fetchGLInfo(): ArrayList<FetchedGlStrings?>? {
        Log.d(TAG, "fetchGLInfo: ")
        val eGL100 = EGLContext.getEGL() as EGL10
        val result = ArrayList<FetchedGlStrings?>()
        val egl10Instance = if (eGL100 == null) null else EGL10Wrapper(eGL100)
        if (eGL100 == null) {
            Log.w(TAG, "Couldn't get EGL")
            return null
        }
        val eglDisplay = eGL100.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        eGL100.eglInitialize(eglDisplay, IntArray(2))
        val numConfig = IntArray(1)
        val configCount =
            if (eGL100.eglGetConfigs(eglDisplay, null, 0, numConfig)) numConfig[0] else 0
        if (configCount <= 0) {
            Log.w(TAG, "Couldn't get EGL config count")
            return null
        }
        var configs: Array<EGLConfig?>? = arrayOfNulls(configCount)
        configs = if (eGL100.eglGetConfigs(
                eglDisplay,
                configs,
                configCount,
                IntArray(1)
            )
        ) configs else null
        if (configs == null) {
            Log.w(TAG, "Couldn't get EGL configs")
            return null
        }
        val arr_v1 = intArrayOf(
            EGL10.EGL_WIDTH,
            EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_HEIGHT,
            EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_NONE
        )
        for (index in 0 until configCount) {
            if (egl10Instance!!.eglGetConfigAttrib(
                    eglDisplay,
                    configs[index],
                    EGL10.EGL_CONFIG_CAVEAT
                ) != 0x3050
                && (egl10Instance.eglGetConfigAttrib(
                    eglDisplay,
                    configs[index],
                    EGL10.EGL_SURFACE_TYPE
                ) and 1) != 0
            ) {
                val attributeValue = egl10Instance.eglGetConfigAttrib(
                    eglDisplay,
                    configs[index],
                    EGL10.EGL_RENDERABLE_TYPE
                )
                if ((attributeValue and 1) != 0) {
                    result.add(
                        buildGLStrings(
                            egl10Instance,
                            eglDisplay,
                            configs[index],
                            arr_v1,
                            null
                        )
                    )
                }

                if ((attributeValue and 4) != 0) {
                    result.add(
                        buildGLStrings(
                            egl10Instance,
                            eglDisplay,
                            configs[index],
                            arr_v1,
                            intArrayOf(0x3098, EGL10.EGL_PIXMAP_BIT, EGL10.EGL_NONE)
                        )
                    )
                }
            }
        }
        egl10Instance!!.eglinstance.eglTerminate(eglDisplay)
        return result
    }

    private fun buildGLStrings(
        egl10Tools: EGL10Wrapper?,
        eglDisplay: EGLDisplay,
        eglConfig: EGLConfig?,
        arr_v: IntArray,
        arr_v1: IntArray?
    ): FetchedGlStrings? {
        val eglContext = egl10Tools!!.eglinstance.eglCreateContext(
            eglDisplay,
            eglConfig,
            EGL10.EGL_NO_CONTEXT,
            arr_v1
        )
        if (eglContext !== EGL10.EGL_NO_CONTEXT) {
            val eglSurface =
                egl10Tools.eglinstance.eglCreatePbufferSurface(eglDisplay, eglConfig, arr_v)
            if (eglSurface === EGL10.EGL_NO_SURFACE) {
                egl10Tools.eglDestroyContext(eglDisplay, eglContext)
                return null
            }
            egl10Tools.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            val result = FetchedGlStrings(0, null, null, null, null)
            val glExtensions = GLES10.glGetString(GLES10.GL_EXTENSIONS)
            if (!TextUtils.isEmpty(glExtensions)) {
                result.glExtensions =
                    glExtensions.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
            }
            result.glRenderer = GLES10.glGetString(GLES10.GL_RENDERER)
            result.glVendor = GLES10.glGetString(GLES10.GL_VENDOR)
            result.glVersion = GLES10.glGetString(GLES10.GL_VERSION)
            if (result.glExtensions != null) {
                egl10Tools.eglMakeCurrent(
                    eglDisplay,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT
                )
                egl10Tools.eglinstance.eglDestroySurface(eglDisplay, eglSurface)
                egl10Tools.eglDestroyContext(eglDisplay, eglContext)
                return result
            }

            val stringBuilder = StringBuilder()

            if (result.glExtensions == null) {
                stringBuilder.append(" glExtensions")
            }
            throw IllegalStateException("Missing required properties:$stringBuilder")
        }
        return null
    }

    fun calculateSHA(data: ByteArray, algorithm: String?): String? {
        val messageDigest0: MessageDigest
        try {
            messageDigest0 = MessageDigest.getInstance(algorithm)
        } catch (noSuchAlgorithmException0: NoSuchAlgorithmException) {
            Log.w(TAG, "[DC] No support for %s?", noSuchAlgorithmException0)
            return null
        }

        messageDigest0.update(data, 0, data.size)
        return Base64.encodeToString(messageDigest0.digest(), 11)
    }

    fun getSystemProperty(key: String?, defaultValue: String?): String? {
        var value = defaultValue
        try {
            @SuppressLint("PrivateApi") val systemPropertiesClass =
                Class.forName("android.os.SystemProperties")
            val getMethod =
                systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            value = getMethod.invoke(null, key, defaultValue) as String
        } catch (e: Exception) {
            Log.w(TAG, "Unable to retrieve system property", e)
        }
        return value
    }

    fun calculatePoint(point: Point, v: Int): Int {
        val f = point.x.toFloat()
        val v1 = ((point.y.toFloat()) * (160.0f / (v.toFloat()))).toInt()
        if (v1 < 470) {
            return 17
        }

        val v2 = (f * (160.0f / (v.toFloat()))).toInt()
        if (v1 >= 960 && v2 >= 720) {
            return if (v1 * 3 / 5 < v2 - 1) 20 else 4
        }

        val v3 = if (v1 < 640 || v2 < 480) 2 else 3
        return if (v1 * 3 / 5 < v2 - 1) v3 or 16 else v3
    }

    class EGL10Wrapper internal constructor(val eglinstance: EGL10) {
        fun eglGetConfigAttrib(eglDisplay: EGLDisplay?, eglConfig: EGLConfig?, v: Int): Int {
            val value = IntArray(1)
            eglinstance.eglGetConfigAttrib(eglDisplay, eglConfig, v, value)
            eglinstance.eglTerminate(eglDisplay)
            return value[0]
        }

        fun eglDestroyContext(eglDisplay: EGLDisplay?, eglContext: EGLContext?) {
            eglinstance.eglDestroyContext(eglDisplay, eglContext)
        }

        fun eglMakeCurrent(
            eglDisplay: EGLDisplay?,
            draw: EGLSurface?,
            read: EGLSurface?,
            eglContext: EGLContext?
        ) {
            eglinstance.eglMakeCurrent(eglDisplay, draw, read, eglContext)
        }
    }


    class FetchedGlStrings(
        var contextClientVersion: Int,
        var glExtensions: Array<String>?,
        var glRenderer: String?,
        var glVendor: String?,
        var glVersion: String?
    )
}
