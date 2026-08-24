/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.inappreview.protocol;

import com.google.android.play.core.inappreview.protocol.IInAppReviewServiceCallback;

interface IInAppReviewService {
    oneway void requestInAppReview(String packageName, in Bundle bundle, in IInAppReviewServiceCallback callback) = 1;
}