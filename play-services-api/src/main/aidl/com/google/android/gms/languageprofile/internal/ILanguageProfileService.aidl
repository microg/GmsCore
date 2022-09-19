package com.google.android.gms.languageprofile.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.languageprofile.ClientLanguageSettings;
import com.google.android.gms.languageprofile.LanguageFluencyParams;
import com.google.android.gms.languageprofile.LanguagePreferenceParams;
import com.google.android.gms.languageprofile.internal.ILanguageProfileCallbacks;

interface ILanguageProfileService {
    String fun1(String accountName) = 0;
    void fun2(String accountName, ILanguageProfileCallbacks callbacks) = 1;
    void getLanguagePreferences(String accountName, in LanguagePreferenceParams params, ILanguageProfileCallbacks callbacks) = 2;
    void getLanguageFluencies(String accountName, in LanguageFluencyParams params, ILanguageProfileCallbacks callbacks) = 3;
    void getLanguageSettings(String accountName, in ClientLanguageSettings settings, IStatusCallback callback) = 4;
    void removeLanguageSettings(String accountName, IStatusCallback callback) = 5;
}
