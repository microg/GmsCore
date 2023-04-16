/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard;

import java.util.Map;

public interface DroidGuardHandle {
    String snapshot(Map<String, String> data);

    boolean isOpened();

    void close();
}
