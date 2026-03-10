/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamite.descriptors.com.google.android.gms.cronet_dynamite;

import java.util.Arrays;
import java.util.List;

public class ModuleDescriptor {
    public static final String MODULE_ID = "com.google.android.gms.cronet_dynamite";
    public static final int MODULE_VERSION = 2;
    public static final List<String> MERGED_CLASSES = Arrays.asList(
            "org.chromium.net.ApiVersion",
            "org.chromium.net.BidirectionalStream",
            "org.chromium.net.CallbackException",
            "org.chromium.net.CronetEngine",
            "org.chromium.net.CronetException",
            "org.chromium.net.CronetProvider",
            "org.chromium.net.ExperimentalBidirectionalStream",
            "org.chromium.net.ExperimentalCronetEngine",
            "org.chromium.net.ExperimentalUrlRequest",
            "org.chromium.net.ICronetEngineBuilder",
            "org.chromium.net.InlineExecutionProhibitedException",
            "org.chromium.net.NetworkException",
            "org.chromium.net.NetworkQualityRttListener",
            "org.chromium.net.NetworkQualityThroughputListener",
            "org.chromium.net.QuicException",
            "org.chromium.net.RequestFinishedInfo",
            "org.chromium.net.UploadDataProvider",
            "org.chromium.net.UploadDataProviders",
            "org.chromium.net.UploadDataSink",
            "org.chromium.net.UrlRequest",
            "org.chromium.net.UrlResponseInfo"
    );
}
