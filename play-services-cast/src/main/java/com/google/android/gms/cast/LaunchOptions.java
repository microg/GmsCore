/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.GoogleApiClient;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Locale;

/**
 * An object that holds options that affect how a receiver application is launched. See
 * {@link Cast.CastApi#launchApplication(GoogleApiClient, String, LaunchOptions)}.
 */
public class LaunchOptions extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    private boolean relaunchIfRunning = false;
    @Field(3)
    private String language;
    @Field(4)
    private boolean androidReceiverCompatible = false;
    @Field(5)
    private CredentialsData credentialsData = null;

    /**
     * The constructor of {@link LaunchOptions}.
     */
    public LaunchOptions() {
        Locale locale = Locale.getDefault();
        StringBuilder sb = new StringBuilder(locale.getLanguage());
        if (!locale.getCountry().isEmpty()) {
            sb.append("-").append(locale.getCountry());
        }
        if (!locale.getVariant().isEmpty()) {
            sb.append("-").append(locale.getVariant());
        }
        language = sb.toString();
    }

    /**
     * Returns {@code true} if the sender app supports casting to an Android TV app.
     */
    public boolean getAndroidReceiverCompatible() {
        return androidReceiverCompatible;
    }

    /**
     * Returns the {@link CredentialsData}.
     */
    public CredentialsData getCredentialsData() {
        return credentialsData;
    }

    /**
     * Returns the language, or {@code null} if none was specified.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Returns the "relaunch if running" flag.
     */
    public boolean getRelaunchIfRunning() {
        return relaunchIfRunning;
    }

    /**
     * Sets the language to be used by the receiver application. If not specified, the sender device's default language is used.
     *
     * @param language The language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Sets the "relaunch if running" flag. If the flag is set, the receiver application will be relaunched even if it is already running. The flag is not set by default.
     */
    public void setRelaunchIfRunning(boolean relaunchIfRunning) {
        this.relaunchIfRunning = relaunchIfRunning;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("LaunchOptions")
                .field("relaunchIfRunning", relaunchIfRunning)
                .field("language", language)
                .field("androidReceiverCompatible", androidReceiverCompatible)
                .end();
    }

    public static Creator<LaunchOptions> CREATOR = new AutoCreator<LaunchOptions>(LaunchOptions.class);
}
