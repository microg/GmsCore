/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.moduleinstall;

/**
 * Listeners to receive updates of module install requests.
 */
public interface InstallStatusListener {
    /**
     * Callback triggered whenever the install status has changed.
     */
    void onInstallStatusUpdated(ModuleInstallStatusUpdate update);
}
