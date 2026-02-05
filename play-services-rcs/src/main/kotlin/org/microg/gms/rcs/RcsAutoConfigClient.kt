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
                
                // Validate that we got actual parsed configuration, not just raw/fallback data
                val hasValidConfig = configurationData.keys.any { key ->
                    key != "raw_response" && key != "xml_response"
                }
                
                if (!hasValidConfig) {
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
        
        try {
            if (responseBody.trim().startsWith("{")) {
                val jsonResponse = JSONObject(responseBody)
                
                val keys = jsonResponse.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = jsonResponse.optString(key, "")
                    if (value.isNotBlank()) {
                        configurationData[key] = value
                    }
                }
            } else if (responseBody.trim().startsWith("<?xml") || responseBody.trim().startsWith("<")) {
                configurationData.putAll(parseXmlConfiguration(responseBody))
            } else {
                Log.w(TAG, "Unknown response format: ${responseBody.take(100)}")
                configurationData["raw_response"] = responseBody
                // REMOVED: Do not treat unknown format as success
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to parse auto-config response", exception)
            configurationData["raw_response"] = responseBody
        }
        
        return configurationData
    }

    private fun parseXmlConfiguration(xmlContent: String): Map<String, String> {
        val configurationData = mutableMapOf<String, String>()
        
        val patterns = listOf(
            "<rcsVersion>([^<]+)</rcsVersion>" to "rcs_version",
            "<imPublicUserIdentity>([^<]+)</imPublicUserIdentity>" to "im_public_user_identity",
            "<realm>([^<]+)</realm>" to "realm",
            "<sipProxy>([^<]+)</sipProxy>" to "sip_proxy",
            "<chatAuth>([^<]+)</chatAuth>" to "chat_auth",
            "<ftAuth>([^<]+)</ftAuth>" to "ft_auth",
            "<MaxSize>([^<]+)</MaxSize>" to "max_file_size",
            "<FtHTTPCSURI>([^<]+)</FtHTTPCSURI>" to "ft_http_cs_uri",
            "<SERVICES>([^<]+)</SERVICES>" to "services"
        )
        
        for ((pattern, key) in patterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val matchResult = regex.find(xmlContent)
            if (matchResult != null) {
                configurationData[key] = matchResult.groupValues[1]
            }
        }
        
        if (configurationData.isEmpty()) {
            configurationData["xml_response"] = "parsed"
        }
        
        return configurationData
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
