/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.protocol;

import org.microg.gms.common.Hide;

@Hide
public class BroadcastConstants {
    public static String ACTION_SESSION_UPDATE = "com.google.android.play.core.assetpacks.receiver.ACTION_SESSION_UPDATE";
    public static String EXTRA_SESSION_STATE = "com.google.android.play.core.assetpacks.receiver.EXTRA_SESSION_STATE";
    public static String EXTRA_FLAGS = "com.google.android.play.core.FLAGS";
    public static String KEY_USING_EXTRACTOR_STREAM = "usingExtractorStream";
}
