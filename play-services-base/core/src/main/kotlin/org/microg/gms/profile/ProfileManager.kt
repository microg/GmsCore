/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XmlResourceParser
import android.util.Log
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.Profile
import org.microg.gms.utils.FileXmlResourceParser
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.*
import kotlin.random.Random

object ProfileManager {
    private const val TAG = "ProfileManager"
    const val PROFILE_REAL = "real"
    const val PROFILE_AUTO = "auto"
    const val PROFILE_NATIVE = "native"
    const val PROFILE_USER = "user"
    const val PROFILE_SYSTEM = "system"

    private var activeProfile: String? = null

    private fun getUserProfileFile(context: Context): File = File(context.filesDir, "device_profile.xml")
    private fun getSystemProfileFile(context: Context): File = File("/system/etc/microg_device_profile.xml")
    private fun getProfileResId(context: Context, profile: String) = context.resources.getIdentifier("${context.packageName}:xml/profile_$profile".toLowerCase(Locale.US), null, null)

    fun getConfiguredProfile(context: Context): String = SettingsContract.getSettings(context, Profile.getContentUri(context), arrayOf(Profile.PROFILE)) { it.getString(0) } ?: PROFILE_AUTO

    fun getAutoProfile(context: Context): String {
        if (hasProfile(context, PROFILE_SYSTEM) && isAutoProfile(context, PROFILE_SYSTEM)) return PROFILE_SYSTEM
        val profile = "${android.os.Build.PRODUCT}_${android.os.Build.VERSION.SDK_INT}"
        if (hasProfile(context, profile) && isAutoProfile(context, profile)) return profile
        return PROFILE_NATIVE
    }

    fun hasProfile(context: Context, profile: String): Boolean = when (profile) {
        PROFILE_AUTO -> hasProfile(context, getAutoProfile(context))
        PROFILE_NATIVE, PROFILE_REAL -> true
        PROFILE_USER -> getUserProfileFile(context).exists()
        PROFILE_SYSTEM -> getSystemProfileFile(context).exists()
        else -> getProfileResId(context, profile) != 0
    }

    private fun getProfileXml(context: Context, profile: String): XmlResourceParser? = kotlin.runCatching {
            when (profile) {
                PROFILE_AUTO -> getProfileXml(context, getAutoProfile(context))
                PROFILE_NATIVE, PROFILE_REAL -> null
                PROFILE_USER -> FileXmlResourceParser(getUserProfileFile(context))
                PROFILE_SYSTEM -> FileXmlResourceParser(getSystemProfileFile(context))
                else -> {
                    val profileResId = getProfileResId(context, profile)
                    if (profileResId == 0) return@runCatching null
                    context.resources.getXml(profileResId)
                }
            }
    }.getOrNull()

    fun isAutoProfile(context: Context, profile: String): Boolean = kotlin.runCatching {
        when (profile) {
            PROFILE_AUTO -> false
            PROFILE_REAL -> false
            PROFILE_NATIVE -> true
            else -> getProfileXml(context, profile)?.use {
                var next = it.next()
                while (next != XmlPullParser.END_DOCUMENT) {
                    when (next) {
                        XmlPullParser.START_TAG -> when (it.name) {
                            "profile" -> {
                                return@use it.getAttributeBooleanValue(null, "auto", false)
                            }
                        }
                    }
                    next = it.next()
                }
            } == true
        }
    }.getOrDefault(false)

    private fun getProfileData(context: Context, profile: String, realData: Map<String, String>): Map<String, String> {
        try {
            if (profile in listOf(PROFILE_REAL, PROFILE_NATIVE)) return realData
            val profileResId = getProfileResId(context, profile)
            if (profileResId == 0) return realData
            val resultData = mutableMapOf<String, String>()
            resultData.putAll(realData)
            getProfileXml(context, profile)?.use {
                var next = it.next()
                while (next != XmlPullParser.END_DOCUMENT) {
                    when (next) {
                        XmlPullParser.START_TAG -> when (it.name) {
                            "data" -> {
                                val key = it.getAttributeValue(null, "key")
                                val value = it.getAttributeValue(null, "value")
                                resultData[key] = value
                                Log.d(TAG, "Overwrite from profile: $key = $value")
                            }
                        }
                    }
                    next = it.next()
                }
            }
            return resultData
        } catch (e: Exception) {
            Log.w(TAG, e)
            return realData
        }
    }

    private fun getProfile(context: Context) = getConfiguredProfile(context).let { if (it != PROFILE_AUTO) it else getAutoProfile(context) }
    private fun getSerialFromSettings(context: Context): String? = SettingsContract.getSettings(context, Profile.getContentUri(context), arrayOf(Profile.SERIAL)) { it.getString(0) }
    private fun saveSerial(context: Context, serial: String) = SettingsContract.setSettings(context, Profile.getContentUri(context)) { put(Profile.SERIAL, serial) }

    private fun randomSerial(template: String, prefixLength: Int = (template.length / 2).coerceAtMost(6)): String {
        val serial = StringBuilder()
        template.forEachIndexed { index, c ->
            serial.append(when {
                index < prefixLength -> c
                c.isDigit() -> '0' + Random.nextInt(10)
                c.isLowerCase() && c <= 'f' -> 'a' + Random.nextInt(6)
                c.isLowerCase() -> 'a' + Random.nextInt(26)
                c.isUpperCase() && c <= 'F' -> 'A' + Random.nextInt(6)
                c.isUpperCase() -> 'A' + Random.nextInt(26)
                else -> c
            })
        }
        return serial.toString()
    }

    @SuppressLint("MissingPermission")
    private fun getProfileSerialTemplate(context: Context, profile: String): String {
        // Native
        if (profile in listOf(PROFILE_REAL, PROFILE_NATIVE)) {
            var candidate = try {
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    android.os.Build.getSerial()
                } else {
                    android.os.Build.SERIAL
                }
            } catch (e: Exception) {
                android.os.Build.SERIAL
            }
            if (candidate != android.os.Build.UNKNOWN) return candidate
        }

        // From profile
        try {
            getProfileXml(context, profile)?.use {
                var next = it.next()
                while (next != XmlPullParser.END_DOCUMENT) {
                    when (next) {
                        XmlPullParser.START_TAG -> when (it.name) {
                            "serial" -> return it.getAttributeValue(null, "template")
                        }
                    }
                    next = it.next()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }

        // Fallback
        return randomSerial("008741A0B2C4D6E8")
    }

    @SuppressLint("MissingPermission")
    fun getSerial(context: Context, profile: String = getProfile(context), local: Boolean = false): String {
        if (!local) getSerialFromSettings(context)?.let { return it }
        val serialTemplate = getProfileSerialTemplate(context, profile)
        val serial = when {
            profile == PROFILE_REAL && serialTemplate != android.os.Build.UNKNOWN -> serialTemplate
            else -> randomSerial(serialTemplate)
        }
        if (!local) saveSerial(context, serial)
        return serial
    }

    private fun getRealData(): Map<String, String> = mutableMapOf(
            "Build.BOARD" to android.os.Build.BOARD,
            "Build.BOOTLOADER" to android.os.Build.BOOTLOADER,
            "Build.BRAND" to android.os.Build.BRAND,
            "Build.CPU_ABI" to android.os.Build.CPU_ABI,
            "Build.CPU_ABI2" to android.os.Build.CPU_ABI2,
            "Build.DEVICE" to android.os.Build.DEVICE,
            "Build.DISPLAY" to android.os.Build.DISPLAY,
            "Build.FINGERPRINT" to android.os.Build.FINGERPRINT,
            "Build.HARDWARE" to android.os.Build.HARDWARE,
            "Build.HOST" to android.os.Build.HOST,
            "Build.ID" to android.os.Build.ID,
            "Build.MANUFACTURER" to android.os.Build.MANUFACTURER,
            "Build.MODEL" to android.os.Build.MODEL,
            "Build.PRODUCT" to android.os.Build.PRODUCT,
            "Build.RADIO" to android.os.Build.RADIO,
            "Build.SERIAL" to android.os.Build.SERIAL,
            "Build.TAGS" to android.os.Build.TAGS,
            "Build.TIME" to android.os.Build.TIME.toString(),
            "Build.TYPE" to android.os.Build.TYPE,
            "Build.USER" to android.os.Build.USER,
            "Build.VERSION.CODENAME" to android.os.Build.VERSION.CODENAME,
            "Build.VERSION.INCREMENTAL" to android.os.Build.VERSION.INCREMENTAL,
            "Build.VERSION.RELEASE" to android.os.Build.VERSION.RELEASE,
            "Build.VERSION.SDK" to android.os.Build.VERSION.SDK,
            "Build.VERSION.SDK_INT" to android.os.Build.VERSION.SDK_INT.toString()
    ).apply {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            put("Build.SUPPORTED_ABIS", android.os.Build.SUPPORTED_ABIS.joinToString(","))
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            put("Build.VERSION.SECURITY_PATCH", android.os.Build.VERSION.SECURITY_PATCH)
        }
    }

    private fun applyProfileData(profileData: Map<String, String>) {
        fun applyStringField(key: String, valueSetter: (String) -> Unit) = profileData[key]?.let { valueSetter(it) }
        fun applyIntField(key: String, valueSetter: (Int) -> Unit) = profileData[key]?.toIntOrNull()?.let { valueSetter(it) }
        fun applyLongField(key: String, valueSetter: (Long) -> Unit) = profileData[key]?.toLongOrNull()?.let { valueSetter(it) }

        applyStringField("Build.BOARD") { Build.BOARD = it }
        applyStringField("Build.BOOTLOADER") { Build.BOOTLOADER = it }
        applyStringField("Build.BRAND") { Build.BRAND = it }
        applyStringField("Build.CPU_ABI") { Build.CPU_ABI = it }
        applyStringField("Build.CPU_ABI2") { Build.CPU_ABI2 = it }
        applyStringField("Build.DEVICE") { Build.DEVICE = it }
        applyStringField("Build.DISPLAY") { Build.DISPLAY = it }
        applyStringField("Build.FINGERPRINT") { Build.FINGERPRINT = it }
        applyStringField("Build.HARDWARE") { Build.HARDWARE = it }
        applyStringField("Build.HOST") { Build.HOST = it }
        applyStringField("Build.ID") { Build.ID = it }
        applyStringField("Build.MANUFACTURER") { Build.MANUFACTURER = it }
        applyStringField("Build.MODEL") { Build.MODEL = it }
        applyStringField("Build.PRODUCT") { Build.PRODUCT = it }
        applyStringField("Build.RADIO") { Build.RADIO = it }
        applyStringField("Build.SERIAL") { Build.SERIAL = it }
        applyStringField("Build.TAGS") { Build.TAGS = it }
        applyLongField("Build.TIME") { Build.TIME = it }
        applyStringField("Build.TYPE") { Build.TYPE = it }
        applyStringField("Build.USER") { Build.USER = it }
        applyStringField("Build.VERSION.CODENAME") { Build.VERSION.CODENAME = it }
        applyStringField("Build.VERSION.INCREMENTAL") { Build.VERSION.INCREMENTAL = it }
        applyStringField("Build.VERSION.RELEASE") { Build.VERSION.RELEASE = it }
        applyStringField("Build.VERSION.SDK") { Build.VERSION.SDK = it }
        applyIntField("Build.VERSION.SDK_INT") { Build.VERSION.SDK_INT = it }
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Build.SUPPORTED_ABIS = profileData["Build.SUPPORTED_ABIS"]?.split(",")?.toTypedArray() ?: emptyArray()
        } else {
            Build.SUPPORTED_ABIS = emptyArray()
        }
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            Build.VERSION.SECURITY_PATCH = profileData["Build.VERSION.SECURITY_PATCH"]
        } else {
            Build.VERSION.SECURITY_PATCH = null
        }
    }

    private fun applyProfile(context: Context, profile: String, serial: String = getSerial(context, profile)) {
        val profileData = getProfileData(context, profile, getRealData())
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            for ((key, value) in profileData) {
                Log.v(TAG, "<data key=\"$key\" value=\"$value\" />")
            }
        }
        applyProfileData(profileData)
        Build.SERIAL = serial
        Log.d(TAG, "Using Serial ${Build.SERIAL}")
        activeProfile = profile
    }

    fun getProfileName(context: Context, profile: String): String? = getProfileName { getProfileXml(context, profile) }

    private fun getProfileName(parserCreator: () -> XmlResourceParser?): String? = parserCreator()?.use {
        var next = it.next()
        while (next != XmlPullParser.END_DOCUMENT) {
            when (next) {
                XmlPullParser.START_TAG -> when (it.name) {
                    "profile" -> {
                        return@use it.getAttributeValue(null, "name")
                    }
                }
            }
            next = it.next()
        }
        null
    }

    fun setProfile(context: Context, profile: String?) {
        val changed = getProfile(context) != profile
        val newProfile = profile ?: PROFILE_AUTO
        val newSerial = if (changed) getSerial(context, newProfile, true) else getSerial(context)
        SettingsContract.setSettings(context, Profile.getContentUri(context)) {
            put(Profile.PROFILE, newProfile)
            if (changed) put(Profile.SERIAL, newSerial)
        }
        if (changed && activeProfile != null) applyProfile(context, newProfile, newSerial)
    }

    fun importUserProfile(context: Context, file: File): Boolean {
        val profileName = getProfileName { FileXmlResourceParser(file) } ?: return false
        try {
            Log.d(TAG, "Importing user profile '$profileName'")
            file.copyTo(getUserProfileFile(context))
            if (activeProfile == PROFILE_USER) applyProfile(context, PROFILE_USER)
            return true
        } catch (e: Exception) {
            Log.w(TAG, e)
            return false
        }
    }

    @JvmStatic
    fun ensureInitialized(context: Context) {
        synchronized(this) {
            try {
                val profile = getProfile(context)
                if (activeProfile == profile) return
                applyProfile(context, profile)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }
}
