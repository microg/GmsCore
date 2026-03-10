/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.languageprofile;

import org.microg.safeparcel.AutoSafeParcelable;

public class ClientLanguageSettings extends AutoSafeParcelable {
    public static final Creator<ClientLanguageSettings> CREATOR = new AutoCreator<>(ClientLanguageSettings.class);
}
