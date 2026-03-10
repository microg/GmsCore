/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcsservice.contacts;

parcelable ImsCapabilities {
    // Base dutk fields - following Google's exact pattern from reverse-engineered code
    boolean isOnline;
    boolean isKnownInNetwork;
    long lastActivityTimestamp;
    int responseCode;
    long validityPeriodMillis;
    Map<String, String> capabilityMetadata;
    List<String> capabilityList;
    String rbmBotStatus;
    
    // ImsCapabilities specific fields
    long timestamp;
    List<String> supportedServiceIdList;
    boolean rcsCapable;
    
    // Service IDs for different RCS features
    const int SERVICE_ID_CHAT = 1;
    const int SERVICE_ID_FILE_TRANSFER = 2;
    const int SERVICE_ID_GROUP_CHAT = 3;
    const int SERVICE_ID_LOCATION_SHARING = 4;
    const int SERVICE_ID_VIDEO_SHARING = 5;
    const int SERVICE_ID_VOICE_MESSAGING = 6;
    const int SERVICE_ID_SMS_FALLBACK = 7;
    const int SERVICE_ID_MMS_FALLBACK = 8;
    const int SERVICE_ID_TYPING_INDICATORS = 9;
    const int SERVICE_ID_MESSAGE_REVOCATION = 10;
    const int SERVICE_ID_MESSAGE_EDITING = 11;
    const int SERVICE_ID_REACTIONS = 12;
    const int SERVICE_ID_E2EE = 13;
    const int SERVICE_ID_GROUP_E2EE = 14;
    const int SERVICE_ID_TACHYGRAM = 15;
    const int SERVICE_ID_VIDEO_CODEC = 16;
    const int SERVICE_ID_IMAGE_CAPTIONS = 17;
    const int SERVICE_ID_PROFILE_SHARING = 18;
    const int SERVICE_ID_BOT_SUPPORT = 19;
    const int SERVICE_ID_GEOPUSH = 20;
}
