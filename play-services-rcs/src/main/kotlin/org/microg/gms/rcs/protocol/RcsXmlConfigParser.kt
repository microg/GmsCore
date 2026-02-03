/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsXmlConfigParser - Carrier configuration XML parser (GSMA RCC.14)
 */

package org.microg.gms.rcs.protocol

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object RcsXmlConfigParser {

    fun parseAutoConfigResponse(xml: String): AutoConfigResult {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            
            val config = mutableMapOf<String, Any>()
            var currentSection = ""
            var currentKey = ""
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        when (tagName) {
                            "characteristic" -> {
                                val type = parser.getAttributeValue(null, "type")
                                if (type != null) {
                                    currentSection = type
                                }
                            }
                            "parm" -> {
                                val name = parser.getAttributeValue(null, "name")
                                val value = parser.getAttributeValue(null, "value")
                                if (name != null && value != null) {
                                    config["$currentSection.$name"] = value
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            return AutoConfigResult(
                isSuccessful = true,
                rcsVersion = config["IMS.version"]?.toString(),
                imsDomain = config["IMS.Home-network-domain"]?.toString(),
                privateUserId = config["IMS.Private-user-identity"]?.toString(),
                publicUserId = config["IMS.Public-user-identity"]?.toString(),
                lboProxy = config["IMS.LBO_P-CSCF-Address"]?.toString(),
                proxyPort = config["IMS.P-CSCF-port"]?.toString()?.toIntOrNull() ?: 5061,
                ftHttpServerUrl = config["Ext.FT-HTTP-Server-Address"]?.toString(),
                ftMaxSize = config["Ext.Max-file-size"]?.toString()?.toLongOrNull() ?: 104857600,
                chatAuthType = config["Ext.ChatAuth"]?.toString()?.toIntOrNull() ?: 1,
                groupChatMaxSize = config["Ext.Max-participants"]?.toString()?.toIntOrNull() ?: 100,
                messagingCapabilities = parseMessagingCaps(config),
                rawConfig = config
            )
            
        } catch (e: Exception) {
            return AutoConfigResult(
                isSuccessful = false,
                errorMessage = e.message
            )
        }
    }

    private fun parseMessagingCaps(config: Map<String, Any>): MessagingCapabilities {
        return MessagingCapabilities(
            chatEnabled = config["SERVICES.ChatAuth"]?.toString()?.toIntOrNull() == 1,
            groupChatEnabled = config["SERVICES.GroupChatAuth"]?.toString()?.toIntOrNull() == 1,
            fileTransferEnabled = config["SERVICES.ftAuth"]?.toString()?.toIntOrNull() == 1,
            fileTransferHttpEnabled = config["SERVICES.ftHTTPAuth"]?.toString()?.toIntOrNull() == 1,
            geoLocationPushEnabled = config["SERVICES.geoLocPushAuth"]?.toString()?.toIntOrNull() == 1,
            standAloneMsgEnabled = config["SERVICES.standaloneMsgAuth"]?.toString()?.toIntOrNull() == 1,
            isComposingEnabled = config["SERVICES.isComposingAuth"]?.toString()?.toIntOrNull() == 1,
            richcallEnabled = config["SERVICES.rcsIPVideoCallAuth"]?.toString()?.toIntOrNull() == 1
        )
    }

    fun buildProvisioningRequest(
        imsi: String,
        imei: String,
        msisdn: String?,
        osVersion: String,
        clientVersion: String,
        clientVendor: String
    ): String {
        val msisdnParam = if (!msisdn.isNullOrEmpty()) {
            """<parm name="msisdn" value="$msisdn"/>"""
        } else ""
        
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <wap-provisioningdoc version="1.1">
                <characteristic type="VERS">
                    <parm name="version" value="1"/>
                    <parm name="validity" value="0"/>
                </characteristic>
                <characteristic type="TOKEN">
                    <parm name="token" value=""/>
                </characteristic>
                <characteristic type="MSG">
                    <parm name="imsi" value="$imsi"/>
                    <parm name="imei" value="$imei"/>
                    $msisdnParam
                    <parm name="default_sms_app" value="1"/>
                    <parm name="os_version" value="$osVersion"/>
                    <parm name="client_version" value="$clientVersion"/>
                    <parm name="client_vendor" value="$clientVendor"/>
                    <parm name="rcs_version" value="UP2.4"/>
                    <parm name="rcs_profile" value="UP2.4"/>
                </characteristic>
            </wap-provisioningdoc>
        """.trimIndent()
    }
}

data class AutoConfigResult(
    val isSuccessful: Boolean,
    val rcsVersion: String? = null,
    val imsDomain: String? = null,
    val privateUserId: String? = null,
    val publicUserId: String? = null,
    val lboProxy: String? = null,
    val proxyPort: Int = 5061,
    val ftHttpServerUrl: String? = null,
    val ftMaxSize: Long = 104857600,
    val chatAuthType: Int = 1,
    val groupChatMaxSize: Int = 100,
    val messagingCapabilities: MessagingCapabilities? = null,
    val rawConfig: Map<String, Any> = emptyMap(),
    val errorMessage: String? = null
)

data class MessagingCapabilities(
    val chatEnabled: Boolean = false,
    val groupChatEnabled: Boolean = false,
    val fileTransferEnabled: Boolean = false,
    val fileTransferHttpEnabled: Boolean = false,
    val geoLocationPushEnabled: Boolean = false,
    val standAloneMsgEnabled: Boolean = false,
    val isComposingEnabled: Boolean = false,
    val richcallEnabled: Boolean = false
)
