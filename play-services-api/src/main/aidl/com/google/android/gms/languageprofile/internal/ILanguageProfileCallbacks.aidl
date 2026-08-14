package com.google.android.gms.languageprofile.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.languageprofile.LanguageFluency;
import com.google.android.gms.languageprofile.LanguagePreference;

interface ILanguageProfileCallbacks {
    oneway void onString(in Status status, String s) = 0;
    oneway void onLanguagePreferences(in Status status, in List<LanguagePreference> preferences) = 1;
    oneway void onLanguageFluencies(in Status status, in List<LanguageFluency> fluencies) = 2;
}
