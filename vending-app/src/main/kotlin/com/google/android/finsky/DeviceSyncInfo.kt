/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky

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
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.os.Build.VERSION.SDK_INT
import android.view.WindowManager
import org.microg.gms.common.Constants
import org.microg.gms.profile.Build
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Objects
import java.util.Random
import java.util.TimeZone
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import kotlin.math.abs

object DeviceSyncInfo {

    private const val TAG = "DeviceSyncInfo"
    private val glInfoList = ArrayList<FetchedGlStrings>()

    fun buildSyncRequest(context: Context, androidId: Long, account: Account): SyncReqWrapper {
        Log.d(TAG, "cachePayload: ")
        val builder = SyncReqWrapper.Builder()
        val payloads = buildPayloads(context, androidId, account)
        val syncRequests = builder.request.toMutableList()
        for (payload in payloads) {
            payload?.run { syncRequests.add(this) }
        }
        builder.request = syncRequests
        return builder.build()
    }

    private fun buildPayloads(context: Context, androidId: Long, account: Account): Array<SyncRequest?> {
        val fetchedGlStrings: ArrayList<FetchedGlStrings> = fetchGLInfo()
        //---------------------------------------GPU info--------------------------------------------------------------------
        val accountSha256 = accountSha256(androidId, account)
        val accountAssValue = AccountAssValue.Builder().value_(accountSha256).build()
        val accountAssociationPayload = AccountAssociationPayload.Builder().accountAss(accountAssValue).build()
        val accountAssociationPayloadRequest = SyncRequest.Builder().accountAssociationPayload(accountAssociationPayload).build()
        //--------------------------------------------------------------------------------------------------------------------
        val carrierPropertiesPayloadRequest = createCarrierPropertiesPayloadRequest(context, androidId)
        val deviceAccountsPayloadRequest = createDeviceAccountsPayloadRequest(context, androidId)
        val deviceInfoCollect = createDeviceInfoCollect(context, fetchedGlStrings.toList())
        val deviceCapabilitiesPayloadRequest = createDeviceCapabilitiesPayloadRequest(deviceInfoCollect)
        val deviceInputPropertiesPayloadRequest = createDeviceInputPropertiesPayloadRequest(deviceInfoCollect)
        val deviceModelPayloadRequest = createDeviceModelPayloadRequest()
        val enterprisePropertiesPayloadRequest = createEnterprisePropertiesPayloadRequest(context)
        val hardwareIdentifierPayloadRequest = createHardwareIdentifierPayloadRequest(context)
        val hardwarePropertiesPayloadRequest = createHardwarePropertiesPayloadRequest(deviceInfoCollect)
        val localePropertiesPayloadRequest = createLocalePropertiesPayloadRequest()
        val playPartnerPropertiesPayloadRequest = createPlayPartnerPropertiesPayloadRequest()
        val playPropertiesPayloadRequest = createPlayPropertiesPayload(context)
        val screenPropertiesPayloadRequest = createScreenPropertiesPayloadRequest(deviceInfoCollect)
        val systemPropertiesPayloadRequest = createSystemPropertiesPayloadRequest(deviceInfoCollect)
        val gpuPayloadRequest = createGpuPayloadRequest(fetchedGlStrings.toList())
        return arrayOf(
            accountAssociationPayloadRequest, carrierPropertiesPayloadRequest, deviceAccountsPayloadRequest,
            deviceCapabilitiesPayloadRequest, deviceInputPropertiesPayloadRequest, deviceModelPayloadRequest,
            enterprisePropertiesPayloadRequest, hardwareIdentifierPayloadRequest, hardwarePropertiesPayloadRequest,
            localePropertiesPayloadRequest, playPartnerPropertiesPayloadRequest, playPropertiesPayloadRequest,
            screenPropertiesPayloadRequest, systemPropertiesPayloadRequest, gpuPayloadRequest
        )
    }

    private fun createDeviceInfoCollect(context: Context, gpuInfoList: List<FetchedGlStrings>): DeviceInfoCollect {
        val builder = DeviceInfoCollect.Builder()
            .reqTouchScreen(0)
            .reqKeyboardType(0)
            .reqNavigation(0)
            .deviceStablePoint(0)
            .reqInputFeaturesV1(false)
            .reqInputFeaturesV2(false)
            .deviceStable(0)
            .reqGlEsVersion(0)
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
            builder.reqInputFeaturesV1((configurationInfo.reqInputFeatures and 1) == 1)
                .reqInputFeaturesV2((configurationInfo.reqInputFeatures and 2) > 0)
        }
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        builder.displayX(size.x).displayY(size.y)
        if (SDK_INT >= 24) {
            builder.deviceStable(DisplayMetrics.DENSITY_DEVICE_STABLE)
                .deviceStablePoint(calculatePoint(size, DisplayMetrics.DENSITY_DEVICE_STABLE))
        }
        val configuration = context.resources.configuration
        builder.screenLayout(configuration.screenLayout)
            .smallestScreenWidthDp(configuration.smallestScreenWidthDp)
            .systemSharedLibraryNames(listOf(*Objects.requireNonNull(context.packageManager.systemSharedLibraryNames)))
            .locales(listOf(*context.assets.locales))
        if (SDK_INT >= 24) {
            builder.glExtensions(gpuInfoList.stream()
                .flatMap { fetchedGlStrings: FetchedGlStrings -> fetchedGlStrings.glExtensions?.let { Arrays.stream(it.toTypedArray()) } }
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
                if (SDK_INT >= 24) {
                    featureInfoProto = FeatureInfoProto.Builder().name(featureInfo.name).version(featureInfo.version).build()
                }
                builder.featureInfoList = builder.featureInfoList.toMutableList().apply {
                    add(featureInfoProto)
                }
                builder.featureNames = builder.featureNames.toMutableList().apply {
                    add(featureInfoProto.name!!)
                }
            }
        }
        if (SDK_INT >= 21) {
            builder.supportedAbi(listOf(*Build.SUPPORTED_ABIS))
        }
        var prop = getSystemProperty("ro.oem.key1", "")
        if (!TextUtils.isEmpty(prop)) {
            builder.oemKey(prop)
        }
        builder.buildCodeName(Build.VERSION.CODENAME)
        prop = getSystemProperty("ro.build.version.preview_sdk_fingerprint", "")
        if (!TextUtils.isEmpty(prop)) {
            builder.previewSdkFingerprint(prop)
        }
        return builder.build()
    }

    private fun createGpuPayloadRequest(glStringsList: List<FetchedGlStrings>): SyncRequest? {
        var gpuPayloadRequest: SyncRequest? = null
        try {
            var infos = glStringsList
            var gpuPayloads = emptyList<GpuPayload>()
            if (SDK_INT >= 24) {
                infos = infos.stream()
                    .filter { fetchedGlStrings: FetchedGlStrings ->
                        fetchedGlStrings.glRenderer!!.isNotEmpty() || fetchedGlStrings.glVendor!!.isNotEmpty() || fetchedGlStrings.glVersion!!.isNotEmpty()
                    }.collect(Collectors.toList())
                val maxVersion = infos.stream()
                    .max(Comparator.comparingInt { fetchedGlStrings: FetchedGlStrings ->
                        fetchedGlStrings.contextClientVersion
                    }).map { obj: FetchedGlStrings ->
                        obj.contextClientVersion
                    }
                if (maxVersion.isPresent) {
                    infos = infos.stream()
                        .filter { fetchedGlStrings: FetchedGlStrings ->
                            fetchedGlStrings.contextClientVersion == maxVersion.get()
                        }.collect(Collectors.toList())
                }
                gpuPayloads = infos.stream().map { fetchedGlStrings: FetchedGlStrings ->
                    val gpuInfoWrapper = GpuInfoWrapper.Builder()
                    if (!TextUtils.isEmpty(fetchedGlStrings.glRenderer)) gpuInfoWrapper.glRenderer(fetchedGlStrings.glRenderer)
                    if (!TextUtils.isEmpty(fetchedGlStrings.glVendor)) gpuInfoWrapper.glVendor(fetchedGlStrings.glVendor)
                    if (!TextUtils.isEmpty(fetchedGlStrings.glVersion)) gpuInfoWrapper.glVersion(fetchedGlStrings.glVersion)
                    GpuPayload.Builder().gpuInfo(gpuInfoWrapper.build()).build()
                }.distinct().collect(Collectors.toList())
            }
            gpuPayloadRequest = SyncRequest.Builder().gpuPayload(if (gpuPayloads.isEmpty()) GpuPayload.Builder().build() else gpuPayloads[0]).build()
        } catch (e: Exception) {
            Log.w(TAG, "createGpuPayloadRequest error", e)
        }
        return gpuPayloadRequest
    }

    private fun createHardwarePropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val hardwarePropertiesPayload = HardwarePropertiesPayload.Builder()
            .isLowRamDevice(deviceInfoCollect.isLowRamDevice)
            .totalMem(deviceInfoCollect.totalMem)
            .availableProcessors(deviceInfoCollect.availableProcessors)
            .supportedAbi(deviceInfoCollect.supportedAbi)
            .build()
        return SyncRequest.Builder().hardwarePropertiesPayload(hardwarePropertiesPayload).build()
    }

    @SuppressLint("DefaultLocale")
    private fun createLocalePropertiesPayloadRequest(): SyncRequest {
        val timeZone = TimeZone.getDefault()
        val gmtFormat = String.format(
            "GMT%+d:%02d",
            timeZone.rawOffset / (60 * 60 * 1000),
            abs(timeZone.rawOffset / (60 * 1000) % 60)
        )
        val localePropertiesPayload = LocalePropertiesPayload.Builder()
            .locale(gmtFormat)
            .build()
        return SyncRequest.Builder().localePropertiesPayload(localePropertiesPayload).build()
    }

    private fun createPlayPartnerPropertiesPayloadRequest(): SyncRequest {
        val playPartnerPropertiesPayload = PlayPartnerPropertiesPayload.Builder()
            .marketId("am-google")
            .partnerIdMs("play-ms-android-google")
            .partnerIdAd("play-ad-ms-android-google")
            .build()
        return SyncRequest.Builder().playPartnerPropertiesPayload(playPartnerPropertiesPayload).build()
    }

    private fun createPlayPropertiesPayload(context: Context): SyncRequest {
        var version = 0
        try {
            version = context.packageManager.getPackageInfo(Constants.VENDING_PACKAGE_NAME, 0).versionCode
        } catch (exception: PackageManager.NameNotFoundException) {
            Log.w(TAG, "[DAS] Could not find our package", exception)
        }
        val playPropertiesPayload = PlayPropertiesPayload.Builder().playVersion(version).build()
        return SyncRequest.Builder().playPropertiesPayload(playPropertiesPayload).build()
    }

    private fun createScreenPropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val screenPropertiesPayload = ScreenPropertiesPayload.Builder()
            .reqTouchScreen(deviceInfoCollect.reqTouchScreen)
            .displayX(deviceInfoCollect.displayX)
            .displayY(deviceInfoCollect.displayY)
            .deviceStablePoint(deviceInfoCollect.deviceStablePoint)
            .deviceStable(deviceInfoCollect.deviceStable)
            .build()
        return SyncRequest.Builder().screenPropertiesPayload(screenPropertiesPayload).build()
    }

    private fun createSystemPropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val systemPropertiesPayload = SystemPropertiesPayload.Builder()
            .fingerprint(Build.FINGERPRINT)
            .sdkInt(Build.VERSION.SDK_INT.toLong())
            .previewSdkFingerprint(deviceInfoCollect.previewSdkFingerprint)
            .buildCodeName(deviceInfoCollect.buildCodeName)
            .oemKey(deviceInfoCollect.oemKey)
            .reqGlEsVersion(deviceInfoCollect.reqGlEsVersion)
            .build()
        return SyncRequest.Builder().systemPropertiesPayload(systemPropertiesPayload).build()
    }

    private fun createHardwareIdentifierPayloadRequest(context: Context): SyncRequest? {
        var hardwareIdentifierPayloadRequest: SyncRequest? = null
        try {
            val builder = HardwareIdentifierPayload.Builder()
            val randomIMEI = generateRandomIMEI()
            val imeId: Long = if (TextUtils.isEmpty(randomIMEI) || !Pattern.compile("^[0-9]{15}$").matcher(randomIMEI).matches())
                0L else randomIMEI.toLong(10) or 0x1000000000000000L
            builder.imeId(imeId)
            hardwareIdentifierPayloadRequest = SyncRequest.Builder().hardwareIdentifierPayload(builder.build()).build()
        } catch (e: Exception) {
            Log.w(TAG, "createHardwareIdentifierPayloadRequest error", e)
        }
        return hardwareIdentifierPayloadRequest
    }

    private fun createEnterprisePropertiesPayloadRequest(context: Context): SyncRequest? {
        var enterprisePropertiesPayloadRequest: SyncRequest? = null
        try {
            val enterprisePropertiesPayload = EnterprisePropertiesPayload.Builder()
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val activeAdmins = devicePolicyManager.activeAdmins
            if (activeAdmins != null) {
                for (componentName in activeAdmins) {
                    val packageName = componentName.packageName
                    val packageInfo: PackageInfo? = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                    val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
                    var isProfileOwner = false
                    if (SDK_INT >= 21) {
                        isProfileOwner = devicePolicyManager.isProfileOwnerApp(packageName)
                    }
                    val policyType =
                        if (isDeviceOwner) MangedScope.MANAGED_DEVICES else if (isProfileOwner) MangedScope.MANAGED_PROFILES else MangedScope.LEGACY_DEVICE_ADMINS
                    val profileInfo = ProfileInfo.Builder()
                        .pkgName(componentName.packageName)
                        .policyType(policyType)
                        .pkgSHA1(calculateSHA(packageInfo!!.signatures[0].toByteArray(), "SHA1"))
                        .pkgSHA256(calculateSHA(packageInfo.signatures[0].toByteArray(), "SHA256")).build()
                    if (isProfileOwner) {
                        enterprisePropertiesPayload.profileOwner(profileInfo)
                    }
                    enterprisePropertiesPayload.default = enterprisePropertiesPayload.default.toMutableList()
                        .apply { add(profileInfo) }
                }
            }
            enterprisePropertiesPayloadRequest = SyncRequest.Builder().enterprisePropertiesPayload(enterprisePropertiesPayload.build()).build()
        } catch (e: Exception) {
            Log.w(TAG, "createEnterprisePropertiesPayloadRequest error", e)
        }
        return enterprisePropertiesPayloadRequest
    }

    private fun createDeviceInputPropertiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val builder = DeviceInputPropertiesPayload.Builder()
            .reqInputFeatures(deviceInfoCollect.reqInputFeaturesV1)
            .reqKeyboardType(deviceInfoCollect.reqKeyboardType)
            .reqNavigation(deviceInfoCollect.reqNavigation)
        return SyncRequest.Builder().deviceInputPropertiesPayload(builder.build()).build()
    }

    private fun createDeviceModelPayloadRequest(): SyncRequest {
        val builder = DeviceModelPayload.Builder()
            .manufacturer(Build.MANUFACTURER)
            .model(Build.MODEL)
            .device(Build.DEVICE)
            .product(Build.PRODUCT)
            .brand(Build.BRAND)
        return SyncRequest.Builder().deviceModelPayload(builder.build()).build()
    }

    private fun createDeviceCapabilitiesPayloadRequest(deviceInfoCollect: DeviceInfoCollect): SyncRequest {
        val builder = DeviceCapabilitiesPayload.Builder()
        builder.glExtensions(deviceInfoCollect.glExtensions)
        val featureInfoList = builder.featureInfo.toMutableList()
        for (featureInfoProto in deviceInfoCollect.featureInfoList) {
            featureInfoList.add(
                FeatureInfoProto.Builder()
                    .name(featureInfoProto.name)
                    .version(featureInfoProto.version)
                    .build()
            )
        }
        builder.featureInfo = featureInfoList
        builder.systemSharedLibraryNames(deviceInfoCollect.systemSharedLibraryNames)
            .locales(deviceInfoCollect.locales)
            .unknownFlag(false)
        return SyncRequest.Builder().deviceCapabilitiesPayload(builder.build()).build()
    }

    private fun createDeviceAccountsPayloadRequest(context: Context, androidId: Long): SyncRequest? {
        var deviceAccountsPayloadRequest: SyncRequest? = null
        try {
            val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
            val accounts = accountManager.accounts
            val builder = DeviceAccountsPayload.Builder()
            val accountAssValues = builder.accountAss.toMutableList()
            for (account in accounts) {
                accountAssValues.add(AccountAssValue.Builder().value_(accountSha256(androidId, account)).build())
            }
            builder.accountAss = accountAssValues
            deviceAccountsPayloadRequest = SyncRequest.Builder().deviceAccountsPayload(builder.build()).build()
        } catch (e: Exception) {
            Log.w(TAG, "createDeviceAccountsPayloadRequest error", e)
        }
        return deviceAccountsPayloadRequest
    }

    @SuppressLint("HardwareIds")
    private fun createCarrierPropertiesPayloadRequest(context: Context, androidId: Long): SyncRequest? {
        var carrierPropertiesPayloadRequest: SyncRequest? = null
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            var simCardId = 0
            if (SDK_INT >= 28) {
                simCardId = telephonyManager.simCarrierId
            }
            var carrierIdFromSimMccMnc = 0
            if (SDK_INT >= 29) {
                carrierIdFromSimMccMnc = telephonyManager.carrierIdFromSimMccMnc
            }
            val telephonyInfo = TelephonyInfo.Builder()
                .subscriberId(androidId)
                .operatorName(telephonyManager.simOperatorName)
                .simCardId(simCardId)
                .carrierIdFromSimMccMnc(carrierIdFromSimMccMnc)
                .build()
            val telephonyStateWrapper = TelephonyStateWrapper.Builder().telephonyInfo(telephonyInfo).build()
            val carrierPropertiesPayload =
                CarrierPropertiesPayload.Builder().telephonyStateValue(telephonyStateWrapper).simOperator(telephonyManager.simOperator).build()
            carrierPropertiesPayloadRequest = SyncRequest.Builder().carrierPropertiesPayload(carrierPropertiesPayload).build()
        } catch (securityException: SecurityException) {
            Log.w(TAG, "SecurityException when reading IMSI.", securityException)
        } catch (stateException: IllegalStateException) {
            Log.w(TAG, "IllegalStateException when reading IMSI. This is a known SDK 31 Samsung bug.", stateException)
        } catch (e: Exception) {
            Log.w(TAG, "createCarrierPropertiesPayloadRequest error", e)
        }
        return carrierPropertiesPayloadRequest
    }

    private fun accountSha256(androidId: Long, account: Account): String? {
        return try {
            val androidIdAcc = (androidId.toString() + "-" + account.name).toByteArray()
            val messageDigest0 = MessageDigest.getInstance("SHA256")
            messageDigest0.update(androidIdAcc, 0, androidIdAcc.size)
            Base64.encodeToString(messageDigest0.digest(), 11)
        } catch (ignored: Exception) {
            null
        }
    }

    private fun generateRandomIMEI(): String {
        val random = Random()
        val imeiBuilder = StringBuilder()
        for (i in 0..13) {
            val digit = random.nextInt(10)
            imeiBuilder.append(digit)
        }
        val imei = imeiBuilder.toString()
        val checkDigit = calculateCheckDigit(imei)
        imeiBuilder.append(checkDigit)
        return imeiBuilder.toString()
    }

    private fun calculateCheckDigit(imei: String): Int {
        var sum = 0
        for (i in imei.indices) {
            var digit = Character.getNumericValue(imei[i])
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

    private fun calculateSHA(data: ByteArray, algorithm: String?): String? {
        val messageDigest0: MessageDigest
        try {
            messageDigest0 = algorithm?.let { MessageDigest.getInstance(it) }!!
        } catch (noSuchAlgorithmException0: NoSuchAlgorithmException) {
            Log.w(TAG, "[DC] No support for %s?", noSuchAlgorithmException0)
            return null
        }
        messageDigest0.update(data, 0, data.size)
        return Base64.encodeToString(messageDigest0.digest(), 11)
    }

    private fun fetchGLInfo(): ArrayList<FetchedGlStrings> {
        if (glInfoList.isNotEmpty()) return glInfoList
        try {
            val eGL100 = EGLContext.getEGL() as? EGL10
            val result = ArrayList<FetchedGlStrings>()
            val egl10Instance = eGL100?.let { EGL10Wrapper(it) }
            val eglDisplay = eGL100!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            eGL100.eglInitialize(eglDisplay, IntArray(2))
            val ints = IntArray(1)
            val configCount = if (eGL100.eglGetConfigs(eglDisplay, null, 0, ints)) ints[0] else 0
            val arrEglConfig = arrayOfNulls<EGLConfig>(configCount)
            val eglConfigs = if (eGL100.eglGetConfigs(eglDisplay, arrEglConfig, configCount, IntArray(1))) arrEglConfig else null
            val arrV1 = intArrayOf(0x3057, 1, 0x3056, 1, 0x3038)
            for (v1 in 0 until configCount) {
                if (egl10Instance?.eglGetConfigAttrib(eglDisplay, eglConfigs?.get(v1), 0x3027) != 0x3050
                    && (egl10Instance?.eglGetConfigAttrib(eglDisplay, eglConfigs?.get(v1), 0x3033)?.and(1)) != 0
                ) {
                    val v2 = egl10Instance?.eglGetConfigAttrib(eglDisplay, eglConfigs?.get(v1), 0x3040)
                    if ((v2?.and(1)) != 0) {
                        egl10Instance?.let { wrapper -> buildGLStrings(wrapper, eglDisplay, eglConfigs?.get(v1), arrV1, null)?.let { result.add(it) } }
                    }
                    if ((v2?.and(4)) != 0) {
                        egl10Instance?.let { wrapper ->
                            buildGLStrings(
                                wrapper,
                                eglDisplay,
                                eglConfigs?.get(v1),
                                arrV1,
                                intArrayOf(0x3098, 2, 0x3038)
                            )?.let { result.add(it) }
                        }
                    }
                }
            }
            egl10Instance?.instance?.eglTerminate(eglDisplay)
            return result.also { glInfoList.addAll(it) }
        } catch (e: Exception) {
            Log.d(TAG, "fetchGLInfo: error", e)
        }
        return ArrayList()
    }

    private fun buildGLStrings(egl10Tools: EGL10Wrapper, eglDisplay: EGLDisplay, eglConfig: EGLConfig?, arrV: IntArray, arrV1: IntArray?): FetchedGlStrings? {
        val eglContext = egl10Tools.instance.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, arrV1)
        if (eglContext != EGL10.EGL_NO_CONTEXT) {
            val eglSurface = egl10Tools.instance.eglCreatePbufferSurface(eglDisplay, eglConfig, arrV)
            if (eglSurface == EGL10.EGL_NO_SURFACE) {
                egl10Tools.eglDestroyContext(eglDisplay, eglContext)
                return null
            }
            egl10Tools.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            val result = FetchedGlStrings(0, null, null, null, null)
            val glExtensions = GLES10.glGetString(GLES10.GL_EXTENSIONS)
            if (!TextUtils.isEmpty(glExtensions)) {
                result.glExtensions = glExtensions.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            }
            result.glRenderer = GLES10.glGetString(GLES10.GL_RENDERER)
            result.glVendor = GLES10.glGetString(GLES10.GL_VENDOR)
            result.glVersion = GLES10.glGetString(GLES10.GL_VERSION)
            if (result.glExtensions != null) {
                egl10Tools.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                egl10Tools.instance.eglDestroySurface(eglDisplay, eglSurface)
                egl10Tools.eglDestroyContext(eglDisplay, eglContext)
                return result
            }
            throw IllegalStateException("Missing required properties <glExtensions>")
        }
        return null
    }

    private fun getSystemProperty(key: String?, defaultValue: String?): String? {
        var value = defaultValue
        try {
            @SuppressLint("PrivateApi") val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            value = getMethod.invoke(null, key, defaultValue) as String
        } catch (e: Exception) {
            Log.w(TAG, "Unable to retrieve system property", e)
        }
        return value
    }

    private fun calculatePoint(point: Point, v: Int): Int {
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

    internal class EGL10Wrapper(val instance: EGL10) {
        fun eglGetConfigAttrib(eglDisplay: EGLDisplay?, eglConfig: EGLConfig?, v: Int): Int {
            val value = IntArray(1)
            instance.eglGetConfigAttrib(eglDisplay, eglConfig, v, value)
            return value[0]
        }

        fun eglDestroyContext(eglDisplay: EGLDisplay?, eglContext: EGLContext?) {
            instance.eglDestroyContext(eglDisplay, eglContext)
        }

        fun eglMakeCurrent(eglDisplay: EGLDisplay?, draw: EGLSurface?, read: EGLSurface?, eglContext: EGLContext?) {
            instance.eglMakeCurrent(eglDisplay, draw, read, eglContext)
        }
    }

    internal class FetchedGlStrings(
        var contextClientVersion: Int,
        var glExtensions: List<String>?,
        var glRenderer: String?,
        var glVendor: String?,
        var glVersion: String?
    )
}