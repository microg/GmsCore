/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.text.internal.client;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.vision.text.internal.client.INativeTextRecognizer;
import com.google.android.gms.vision.text.internal.client.TextRecognizerOptions;

interface INativeTextRecognizerCreator {
    INativeTextRecognizer create(IObjectWrapper context, in TextRecognizerOptions options) = 0;
}
