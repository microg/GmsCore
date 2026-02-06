/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.libraries.entitlement.utils;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/** Wraps the TS.43 XML raw string and parses it into nodes. */
public final class Ts43XmlDoc {
    private static final String TAG = "Ts43XmlDoc";
    private static final String NODE_CHARACTERISTIC = "characteristic";
    private static final String NODE_PARM = "parm";
    private static final String PARM_NAME = "name";
    private static final String PARM_VALUE = "value";

    /** Type names of characteristics. */
    public static final class CharacteristicType {
        private CharacteristicType() {
        }

        public static final String APPLICATION = "APPLICATION";
        public static final String PRIMARY_CONFIGURATION = "PrimaryConfiguration";
        public static final String COMPANION_CONFIGURATIONS = "CompanionConfigurations";
        public static final String COMPANION_CONFIGURATION = "CompanionConfiguration";
        public static final String ENTERPRISE_CONFIGURATION = "EnterpriseConfiguration";
        public static final String USER = "USER";
        public static final String TOKEN = "TOKEN";
        public static final String DOWNLOAD_INFO = "DownloadInfo";
        public static final String MSG = "MSG";
    }

    /** Names of parameters. */
    public static final class Parm {
        private Parm() {
        }

        public static final String TOKEN = "token";
        public static final String APP_ID = "AppID";
        public static final String VERSION = "version";
        public static final String VALIDITY = "validity";
        public static final String OPERATION_RESULT = "OperationResult";
        public static final String GENERAL_ERROR_URL = "GeneralErrorURL";
        public static final String GENERAL_ERROR_USER_DATA = "GeneralErrorUserData";
        public static final String GENERAL_ERROR_TEXT = "GeneralErrorText";
        public static final String PRIMARY_APP_ELIGIBILITY = "PrimaryAppEligibility";
        public static final String COMPANION_APP_ELIGIBILITY = "CompanionAppEligibility";
        public static final String ENTERPRISE_APP_ELIGIBILITY = "EnterpriseAppEligibility";
        public static final String NOT_ENABLED_URL = "NotEnabledURL";
        public static final String NOT_ENABLED_USER_DATA = "NotEnabledUserData";
        public static final String NOT_ENABLED_CONTENTS_TYPE = "NotEnabledContentsType";
        public static final String COMPANION_DEVICE_SERVICES = "CompanionDeviceServices";
        public static final String TEMPORARY_TOKEN = "TemporaryToken";
        public static final String TEMPORARY_TOKEN_EXPIRY = "TemporaryTokenExpiry";
        public static final String MSISDN = "msisdn";
        public static final String ICCID = "ICCID";
        public static final String SERVICE_STATUS = "ServiceStatus";
        public static final String POLLING_INTERVAL = "PollingInterval";
        public static final String SUBSCRIPTION_RESULT = "SubscriptionResult";
        public static final String SUBSCRIPTION_SERVICE_URL = "SubscriptionServiceURL";
        public static final String SUBSCRIPTION_SERVICE_USER_DATA = "SubscriptionServiceUserData";
        public static final String SUBSCRIPTION_SERVICE_CONTENTS_TYPE =
                "SubscriptionServiceContentsType";
        public static final String PROFILE_ACTIVATION_CODE = "ProfileActivationCode";
        public static final String PROFILE_ICCID = "ProfileIccid";
        public static final String PROFILE_SMDP_ADDRESS = "ProfileSmdpAddress";
        public static final String OPERATION_TARGETS = "OperationTargets";
        public static final String MESSAGE = "Message";
        public static final String ACCEPT_BUTTON = "Accept_btn";
        public static final String ACCEPT_BUTTON_LABEL = "Accept_btn_label";
        public static final String REJECT_BUTTON = "Reject_btn";
        public static final String REJECT_BUTTON_LABEL = "Reject_btn_label";
        public static final String ACCEPT_FREETEXT = "Accept_freetext";
    }

    /** Parameter values of XML response content. */
    public static final class ParmValues {
        private ParmValues() {
        }

        public static final String OPERATION_RESULT_SUCCESS = "1";
        public static final String OPERATION_RESULT_ERROR_GENERAL = "100";
        public static final String OPERATION_RESULT_ERROR_INVALID_OPERATION = "101";
        public static final String OPERATION_RESULT_ERROR_INVALID_PARAMETER = "102";
        public static final String OPERATION_RESULT_WARNING_NOT_SUPPORTED_OPERATION = "103";
        public static final String OPERATION_RESULT_ERROR_INVALID_MSG_RESPONSE = "104";
        public static final String PRIMARY_APP_ELIGIBILITY_ENABLED = "1";
        public static final String SERVICE_STATUS_ACTIVATED = "1";
        public static final String SERVICE_STATUS_ACTIVATING = "2";
        public static final String SERVICE_STATUS_DEACTIVATED = "3";
        public static final String SERVICE_STATUS_DEACTIVATED_NO_REUSE = "4";
        public static final String SUBSCRIPTION_RESULT_CONTINUE_TO_WEBSHEET = "1";
        public static final String SUBSCRIPTION_RESULT_DOWNLOAD_PROFILE = "2";
        public static final String SUBSCRIPTION_RESULT_DONE = "3";
        public static final String SUBSCRIPTION_RESULT_DELAYED_DOWNLOAD = "4";
        public static final String SUBSCRIPTION_RESULT_DISMISS = "5";
        public static final String SUBSCRIPTION_RESULT_DELETE_PROFILE_IN_USE = "6";
        public static final String SUBSCRIPTION_RESULT_REDOWNLOADABLE_PROFILE_IS_MANDATORY = "7";
        public static final String SUBSCRIPTION_RESULT_REQUIRES_USER_INPUT = "8";
        public static final String CONTENTS_TYPE_XML = "xml";
        public static final String CONTENTS_TYPE_JSON = "json";
        public static final String DISABLED = "0";
        public static final String ENABLED = "1";
        public static final String INCOMPATIBLE = "2";
    }

    /**
     * Maps characteristics to a map of parameters. Key is the characteristic type. Value is
     * parameter
     * name and value. Example: {"APPLICATION" -> {"AppId" -> "ap2009", "OperationResult" -> "1"},
     * "APPLICATION|PrimaryConfiguration" -> {"ICCID" -> "123", "ServiceStatus" -> "2",
     * "PollingInterval" -> "1"} }
     */
    private final Map<String, Map<String, String>> mCharacteristicsMap = new ArrayMap<>();

    public Ts43XmlDoc(String responseBody) {
        parseXmlResponse(responseBody);
    }

    /** Returns {@code true} if a node structure exists for a given characteristicTypes. */
    public boolean contains(ImmutableList<String> characteristicTypes) {
        return mCharacteristicsMap.containsKey(TextUtils.join("|", characteristicTypes));
    }

    /**
     * Returns param value for given characteristicType and parameterName, or {@code null} if not
     * found.
     */
    @Nullable
    public String get(ImmutableList<String> characteristicTypes, String parameterName) {
        Map<String, String> parmMap = mCharacteristicsMap.get(
                TextUtils.join("|", characteristicTypes));
        return parmMap == null ? null : parmMap.get(parameterName.toLowerCase(Locale.ROOT));
    }

    /**
     * Parses the response body as per format defined in TS.43 New Characteristics for XML-Based
     * Document.
     */
    private void parseXmlResponse(String responseBody) {
        if (responseBody == null) {
            return;
        }
        // Workaround: some server doesn't escape "&" in XML response and that will cause XML parser
        // failure later.
        // This is a quick impl of escaping w/o introducing a ton of new dependencies.
        responseBody = responseBody.replace("&", "&amp;").replace("&amp;amp;", "&amp;");
        try {
            InputSource inputSource = new InputSource(new StringReader(responseBody));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                parseNode(new ArrayList<>(), Objects.requireNonNull(nodeList.item(i)));
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            // Nodes that failed to parse won't be stored in nodesMap
            Log.w(TAG, "e=" + e);
        }
    }

    @SuppressWarnings("AndroidJdkLibsChecker") // java.util.Map#getOrDefault
    /* Parses characteristics and parm values into characteristicsMap. */
    private void parseNode(List<String> characteristics, Node node) {
        String nodeName = node.getNodeName();
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return;
        }
        if (nodeName.equals(NODE_CHARACTERISTIC)) {
            Node typeNode = attributes.getNamedItem("type");
            if (typeNode == null) {
                return;
            }
            characteristics.add(Objects.requireNonNull(typeNode.getNodeValue()));
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                parseNode(characteristics, Objects.requireNonNull(children.item(i)));
            }
            characteristics.remove(characteristics.size() - 1);
        } else if (nodeName.equals(NODE_PARM)) {
            Node parmNameNode = attributes.getNamedItem(PARM_NAME);
            Node parmValueNode = attributes.getNamedItem(PARM_VALUE);
            if (parmNameNode == null || parmValueNode == null) {
                return;
            }
            String characteristicKey = TextUtils.join("|", characteristics);
            Map<String, String> parmMap =
                    mCharacteristicsMap.getOrDefault(characteristicKey, new ArrayMap<>());
            parmMap.put(
                    Objects.requireNonNull(parmNameNode.getNodeValue().toLowerCase(Locale.ROOT)),
                    Objects.requireNonNull(parmValueNode.getNodeValue()));
            mCharacteristicsMap.put(characteristicKey, parmMap);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Ts43XmlDoc: " + mCharacteristicsMap;
    }
}
