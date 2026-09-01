/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.languageprofile;

import org.microg.safeparcel.AutoSafeParcelable;

public class LanguageFluency extends AutoSafeParcelable {
    public static final Creator<LanguageFluency> CREATOR = new AutoCreator<>(LanguageFluency.class);
}
