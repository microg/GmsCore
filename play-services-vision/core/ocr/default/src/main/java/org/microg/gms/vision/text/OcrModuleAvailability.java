/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text;

import android.content.Context;

import org.microg.gms.vision.text.paddle.model.OcrModelAccess;

public final class OcrModuleAvailability {
    private OcrModuleAvailability() {
    }

    public static boolean isAvailable(Context context, String pack) {
        return OcrModelAccess.isAvailable(context, pack);
    }
}
