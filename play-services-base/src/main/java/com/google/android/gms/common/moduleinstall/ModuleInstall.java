/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

import android.app.Activity;
import android.content.Context;

/**
 * The main entry point for module install services.
 */
public class ModuleInstall {
    /**
     * Creates a new instance of {@link ModuleInstallClient} for use in an {@link Activity}.
     */
    public static ModuleInstallClient getClient(Activity activity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new instance of {@link ModuleInstallClient} for use in a non-activity {@link Context}.
     */
    public static ModuleInstallClient getClient(Context context) {
        throw new UnsupportedOperationException();
    }
}
