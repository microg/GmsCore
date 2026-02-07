/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsAutoConfigClient - Auto-configuration protocol client
 * 
 * Implements RCS auto-configuration (ACS) protocol per GSMA specification.
 * Handles HTTP-based provisioning with carrier servers.
 */

package org.microg.gms.rcs

import android.util.Log
import com.google.android.gms.rcs.RcsConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object RcsAutoConfigClient {

    private const val TAG = "RcsAutoConfig"
    
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun fetchConfiguration(
        autoConfigUrl: String,
        phoneNumber: String?,
        existingConfiguration: RcsConfiguration
    ): AutoConfigResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching auto-configuration from: $autoConfigUrl")

                val requestBody = buildAutoConfigRequest(phoneNumber, existingConfiguration)
                
                val request = Request.Builder()
                    .url(autoConfigUrl)
                    .post(requestBody)
                    // GSMA RCC.07 §2.4.4 - Required Headers for Auto-Config
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("User-Agent", "microG-RCS/1.0 (Android; Open Source)")
                    .addHeader("Accept", "application/vnd.gsma.rcs-config-xml, application/json")
                    .addHeader("Cache-Control", "no-cache") 
                    .build()

                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.w(TAG, "Auto-config request failed with code: ${response.code}")
                    return@withContext AutoConfigResponse(
                        isSuccessful = false,
                        errorCode = response.code,
                        errorMessage = "Server returned error: ${response.code}"
                    )
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    return@withContext AutoConfigResponse(
                        isSuccessful = false,
                        errorCode = -1,
                        errorMessage = "Empty response from server"
                    )
                }

                val configurationData = parseAutoConfigResponse(responseBody)
                
                if (configurationData.isEmpty()) {
                    Log.w(TAG, "Failed to parse configuration from response")
                    return@withContext AutoConfigResponse(
                        isSuccessful = false,
                        errorCode = -2,
                        errorMessage = "Failed to parse configuration response"
                    )
                }
                
                Log.d(TAG, "Auto-configuration received successfully")
                
                AutoConfigResponse(
                    isSuccessful = true,
                    errorCode = 0,
                    errorMessage = "",
                    configurationData = configurationData
                )

            } catch (exception: Exception) {
                Log.e(TAG, "Auto-configuration request failed", exception)
                
                AutoConfigResponse(
                    isSuccessful = false,
                    errorCode = -1,
                    errorMessage = "Network error: ${exception.message}"
                )
            }
        }
    }

    private fun buildAutoConfigRequest(
        phoneNumber: String?,
        configuration: RcsConfiguration
    ): FormBody {
        val builder = FormBody.Builder()
        
        builder.add("rcs_version", configuration.rcsVersion ?: "UP2.4")
        builder.add("rcs_profile", configuration.rcsProfile ?: "UP2.4")
        builder.add("client_vendor", configuration.clientVendor ?: "microG")
        builder.add("client_version", configuration.clientVersion ?: "1.0.0")
        builder.add("terminal_vendor", android.os.Build.MANUFACTURER)
        builder.add("terminal_model", android.os.Build.MODEL)
        builder.add("terminal_sw_version", android.os.Build.VERSION.RELEASE)
        
        if (!phoneNumber.isNullOrBlank()) {
            builder.add("msisdn", phoneNumber)
        }

        return builder.build()
    }

    private fun parseAutoConfigResponse(responseBody: String): Map<String, String> {
        val configurationData = mutableMapOf<String, String>()
        val trimmedBody = responseBody.trim()
        
        try {
            if (trimmedBody.startsWith("{")) {
                val jsonResponse = JSONObject(responseBody)
                
                val keys = jsonResponse.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = jsonResponse.optString(key, "")
                    if (value.isNotBlank()) {
                        configurationData[key] = value
                    }
                }
            } else if (trimmedBody.startsWith("<") || trimmedBody.contains("<wap-provisioningdoc")) {
                // Handle XML (both standard and WAP provisioning formats)
                configurationData.putAll(parseXmlConfiguration(responseBody))
            } else {
                Log.w(TAG, "Unknown response format (not JSON or XML), length: ${responseBody.length}")
                // Do not attempt to guess or parse unknown formats.
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to parse auto-config response", exception)
        }
        
        return configurationData
    }

    private fun parseXmlConfiguration(xmlContent: String): Map<String, String> {
        val configurationData = mutableMapOf<String, String>()
        
        try {
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(java.io.StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTag: String? = null
            var inCharacteristic = false
            var typeAttribute: String? = null

            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("characteristic", ignoreCase = true)) {
                            inCharacteristic = true
                            typeAttribute = parser.getAttributeValue(null, "type")
                        }
                        
                        // Handle Parm fields (common in OMA CP / WAP provisioning)
                        if (currentTag.equals("parm", ignoreCase = true)) {
                            val name = parser.getAttributeValue(null, "name")
                            val value = parser.getAttributeValue(null, "value")
                            if (!name.isNullOrBlank() && !value.isNullOrBlank()) {
                                mapProvisioningParam(name, value, configurationData)
                            }
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        if (currentTag != null && parser.text != null) {
                            val text = parser.text.trim()
                            if (text.isNotEmpty()) {
                                when (currentTag) {
                                    // Direct XML tags (GSMA RCC.14/07)
                                    "rcsVersion" -> configurationData["rcs_version"] = text
                                    "imPublicUserIdentity" -> configurationData["im_public_user_identity"] = text
                                    "realm" -> configurationData["realm"] = text
                                    "sipProxy" -> configurationData["sip_proxy"] = text
                                    "chatAuth" -> configurationData["chat_auth"] = text
                                    "ftAuth" -> configurationData["ft_auth"] = text
                                    "MaxSize" -> configurationData["max_file_size"] = text
                                    "FtHTTPCSURI" -> configurationData["ft_http_cs_uri"] = text
                                    "Token" -> configurationData["token"] = text
                                    "validity" -> configurationData["validity"] = text
                                }
                            }
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name.equals("characteristic", ignoreCase = true)) {
                             inCharacteristic = false
                             typeAttribute = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML configuration", e)
        }
        
        if (configurationData.isEmpty()) {
            Log.w(TAG, "XML parsing yielded no configuration")
            Log.d(TAG, "Failed XML content sample: ${xmlContent.take(200)}")
        }
        
        return configurationData
    }

    private fun mapProvisioningParam(name: String, value: String, data: MutableMap<String, String>) {
        // Map OMA DM/CP 'parm' names to our internal keys
        when (name.lowercase()) {
            "rcsversion" -> data["rcs_version"] = value
            "impublicuseridentity" -> data["im_public_user_identity"] = value
            "realm", "domain" -> data["realm"] = value
            "sipproxy", "appaddr" -> data["sip_proxy"] = value // AppAddr often used for proxy
            "chatauth", "appauth" -> data["chat_auth"] = value
            "ftauth" -> data["ft_auth"] = value
            "maxsize" -> data["max_file_size"] = value
            "fthttpcsuri" -> data["ft_http_cs_uri"] = value
            "token" -> data["token"] = value
            "validity" -> data["validity"] = value
        }
    }

    suspend fun verifyPhoneNumber(
        verificationUrl: String,
        phoneNumber: String,
        verificationCode: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Verifying phone number: ${maskPhoneNumber(phoneNumber)}")

                val requestBody = FormBody.Builder()
                    .add("msisdn", phoneNumber)
                    .add("otp", verificationCode)
                    .build()

                val request = Request.Builder()
                    .url(verificationUrl)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("User-Agent", "microG-RCS/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                
                response.isSuccessful

            } catch (exception: Exception) {
                Log.e(TAG, "Phone verification failed", exception)
                false
            }
        }
    }

    private fun maskPhoneNumber(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank() || phoneNumber.length < 4) {
            return "***"
        }
        
        val visibleDigits = 4
        val maskedLength = phoneNumber.length - visibleDigits
        val mask = "*".repeat(maskedLength)
        
        return mask + phoneNumber.takeLast(visibleDigits)
    }
}
