/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.appset;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;

/**
 * A client for interacting with the {@link AppSetIdInfo} API.
 */
public interface AppSetIdClient {
    /**
     * Gets the AppSetIdInfo asynchronously.
     *
     * @return a {@link Task} of the returned {@link AppSetIdInfo}.
     */
    @NonNull
    Task<AppSetIdInfo> getAppSetIdInfo();
}
