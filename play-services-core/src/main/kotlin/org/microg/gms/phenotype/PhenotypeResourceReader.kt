/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Base64
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import java.io.IOException
import okio.ByteString.Companion.toByteString
import org.xmlpull.v1.XmlPullParser

internal class PhenotypeResourceReader(context: Context) {
    companion object {
        private const val TAG = "PhenotypeResReader"
        private const val XML_METADATA = "$PHENOTYPE_CONTENT_AUTHORITY.registration.xml"
        private const val BINARY_METADATA = "$PHENOTYPE_CONTENT_AUTHORITY.registration.binarypb:"
        private const val HETERODYNE_METADATA = "$PHENOTYPE_CONTENT_AUTHORITY.heterodyne_info.binarypb:"
        private const val METADATA_SERVICE = "com.google.android.libraries.phenotype.registration.PhenotypeMetadataHolderService"
        private const val RESOURCE_FLAGS = PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_DIRECT_BOOT_AWARE or PackageManager.MATCH_DIRECT_BOOT_UNAWARE

        fun buildConfigPackageName(configPackage: String, androidPackage: String, autoSubpackage: Boolean): String {
            require(configPackage.isNotEmpty()) { "Empty configuration package" }
            if (!autoSubpackage) return configPackage
            require('#' !in configPackage) { "Auto-subpackage requires a configuration package without '#'" }
            return "$configPackage#$androidPackage"
        }
    }

    private val packageManager = context.packageManager

    fun registrationPackageNames(): List<String> = packageManager.queryIntentServices(
        Intent(METADATA_SERVICE), RESOURCE_FLAGS
    ).mapNotNull { it.serviceInfo?.packageName }.distinct()

    fun computeBaselineCl(packageName: String): Long = try {
        readBaselineCl(packageName)
    } catch (e: IOException) {
        Log.w(TAG, "Failed to read build data for $packageName", e)
        -1L
    }

    fun readBaselineCl(packageName: String): Long {
        try {
            val services = packageManager.queryIntentServices(
                Intent("com.google.android.build.data.Properties").setPackage(packageName), RESOURCE_FLAGS
            )
            if (services.isEmpty()) return -1L
            if (services.size != 1) throw IOException("Ambiguous build data service")
            val resourceId = services.single().serviceInfo?.metaData
                ?.getInt("com.google.android.build.data.properties") ?: 0
            if (resourceId == 0) return -1L
            return packageManager.getResourcesForApplication(packageName).openRawResource(resourceId).use {
                AndroidBuildDataProperties.ADAPTER.decode(it).baselineCl ?: -1L
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return -1L
        }
    }

    fun readRegistrations(packageInfo: PackageInfo): List<DeclarativeRegistration> =
        readRegistrations(packageInfo, binaryResources(packageInfo.packageName))

    fun readRegistration(androidPackage: String, configPackage: String): DeclarativeRegistration? {
        try {
            val namespace = configPackage.substringBefore('#')
            val info = packageManager.getPackageInfo(androidPackage, PackageManager.GET_META_DATA)
            val binary = binaryResources(androidPackage)
            val ids = binary[namespace]
            if (ids != null) {
                return readBinary(
                    packageManager.getResourcesForApplication(androidPackage), info,
                    namespace, ids, computeBaselineCl(androidPackage)
                )
            }
            return readRegistrations(info, binary).firstOrNull { it.configPackage == configPackage }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read registration for $androidPackage", e)
            return null
        }
    }

    private fun readRegistrations(
        packageInfo: PackageInfo,
        binary: Map<String, Pair<Int, Int>>
    ): List<DeclarativeRegistration> {
        val packageName = packageInfo.packageName
        val registrations = mutableListOf<DeclarativeRegistration>()
        val binaryPackages = mutableSetOf<String>()
        val binaryNamespaces = mutableSetOf<String>()
        val resources by lazy { packageManager.getResourcesForApplication(packageName) }
        val baselineCl by lazy { computeBaselineCl(packageName) }
        for ((namespace, ids) in binary) {
            val registration = readBinary(resources, packageInfo, namespace, ids, baselineCl)
            registrations.add(registration)
            binaryPackages.add(registration.configPackage!!)
            binaryNamespaces.add(namespace)
        }
        val metadata = packageInfo.applicationInfo?.metaData ?: return registrations
        for (key in metadata.keySet()) {
            if (key != XML_METADATA && !key.startsWith("$XML_METADATA:")) continue
            if (key.startsWith("$XML_METADATA:") && key.substringAfter(':') in binaryNamespaces) continue
            val resourceId = metadata.getInt(key, 0)
            if (resourceId == 0) continue
            registrations.addAll(
                readXml(resources, packageInfo, resourceId, baselineCl)
                    .filter { it.configPackage !in binaryPackages })
        }
        return registrations
    }

    private fun binaryResources(packageName: String): Map<String, Pair<Int, Int>> {
        val metadata = packageManager.queryIntentServices(Intent(METADATA_SERVICE).setPackage(packageName), RESOURCE_FLAGS)
            .firstOrNull { it.serviceInfo?.name == METADATA_SERVICE && it.serviceInfo.metaData != null }
            ?.serviceInfo?.metaData ?: return emptyMap()
        val registrations = mutableMapOf<String, Int>()
        val heterodyne = mutableMapOf<String, Int>()
        for (key in metadata.keySet()) {
            val target = when {
                key.startsWith(BINARY_METADATA) -> registrations
                key.startsWith(HETERODYNE_METADATA) -> heterodyne
                else -> continue
            }
            val resourceId = metadata.getInt(key, 0)
            if (resourceId != 0) target[key.substringAfter(':').substringBefore(':')] = resourceId
        }
        return registrations.mapValues { (namespace, id) -> id to (heterodyne[namespace] ?: 0) }
    }

    private fun readBinary(
        resources: Resources, info: PackageInfo, namespace: String, ids: Pair<Int, Int>,
        baselineCl: Long
    ): DeclarativeRegistration {
        val registration = resources.openRawResource(ids.first).use { DeclarativeRegistration.ADAPTER.decode(it) }
        require(registration.configPackage.orEmpty().substringBefore('#') == namespace) {
            "Registration namespace does not match resource metadata"
        }
        val configPackage = buildConfigPackageName(
            registration.configPackage.orEmpty(), info.packageName,
            registration.autoSubpackage == true
        )
        val heterodyneInfo = if (ids.second == 0) registration.heterodyneInfo else {
            val resource = resources.openRawResource(ids.second).use { RegistrationHeterodyneInfo.ADAPTER.decode(it) }
            require(resource.configPackage == namespace) { "Heterodyne namespace does not match resource metadata" }
            resource.copy(configPackage = null).encodeByteString()
        }
        val version = registration.version?.takeUnless { it == 0 }
        return registration.copy(
            configPackage = configPackage, androidPackage = info.packageName, field10 = null,
            version = version, versionCode = registration.versionCode
                ?: if (version == null) PackageInfoCompat.getLongVersionCode(info) else null, baselineCl = baselineCl,
            registrationFormat = DeclarativeRegistration.Format.BINARY, heterodyneInfo = heterodyneInfo
        )
    }

    private fun readXml(
        resources: Resources, info: PackageInfo, resourceId: Int,
        baselineCl: Long
    ): List<DeclarativeRegistration> {
        val registrations = mutableListOf<DeclarativeRegistration>()
        resources.getXml(resourceId).use { xml ->
            xml.nextTag()
            xml.require(XmlPullParser.START_TAG, null, "phenotype-registrations")
            while (xml.nextTag() != XmlPullParser.END_TAG) {
                val child = xml.name
                if (child == "phenotype-registration") registrations.add(parseRegistration(xml, info, baselineCl))
                else skipTag(xml)
                xml.require(XmlPullParser.END_TAG, null, child)
            }
            xml.require(XmlPullParser.END_TAG, null, "phenotype-registrations")
        }
        return registrations
    }

    private fun parseRegistration(xml: XmlResourceParser, info: PackageInfo, baselineCl: Long): DeclarativeRegistration {
        val builder = DeclarativeRegistration.Builder()
        var autoSubpackage = false
        while (xml.nextTag() != XmlPullParser.END_TAG) {
            val tag = xml.name
            when (tag) {
                "configuration-package" -> builder.configPackage(xml.nextText())
                "configuration-version" -> builder.version(xml.nextText().toInt())
                "auto-subpackage" -> autoSubpackage = xml.nextText().toBoolean()
                "delete-runtime-properties" -> builder.deleteRuntimeProperties(xml.nextText().toBoolean())
                "params" -> builder.params(Base64.decode(xml.nextText(), Base64.URL_SAFE).toByteString())
                "log-sources" -> {
                    val sources = mutableListOf<String>()
                    while (xml.nextTag() != XmlPullParser.END_TAG) {
                        val child = xml.name
                        if (child == "log-source") sources.add(xml.nextText()) else skipTag(xml)
                        xml.require(XmlPullParser.END_TAG, null, child)
                    }
                    builder.logSources(builder.logSources + sources)
                }

                else -> skipTag(xml)
            }
            xml.require(XmlPullParser.END_TAG, null, tag)
        }
        xml.require(XmlPullParser.END_TAG, null, "phenotype-registration")
        builder.configPackage(buildConfigPackageName(builder.configPackage.orEmpty(), info.packageName, autoSubpackage))
        if (builder.version == null || builder.version == 0) {
            builder.version(null).versionCode(PackageInfoCompat.getLongVersionCode(info))
        }
        return builder.androidPackage(info.packageName).registrationSource(RegistrationSource.RESOURCE)
            .registrationFormat(DeclarativeRegistration.Format.XML).baselineCl(baselineCl).build()
    }

    private fun skipTag(xml: XmlResourceParser) {
        val tag = xml.name
        xml.require(XmlPullParser.START_TAG, null, tag)
        var depth = 1
        while (depth > 0) {
            when (xml.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> throw IllegalArgumentException("Unexpected end of XML")
            }
        }
        xml.require(XmlPullParser.END_TAG, null, tag)
    }
}
