/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.firebase.auth;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Structure that contains the required continue/state URL with optional Android and iOS bundle identifiers.
 * The stateUrl used to initialize this class is the link/deep link/fallback url used while constructing the Firebase dynamic link.
 */
@PublicApi
public class ActionCodeSettings extends AutoSafeParcelable {
    @Field(1)
    @PublicApi(exclude = true)
    public String url;
    @Field(2)
    @PublicApi(exclude = true)
    public String iOSBundle;
    @Field(3)
    @PublicApi(exclude = true)
    public String iOSAppStoreId;
    @Field(4)
    @PublicApi(exclude = true)
    public String androidPackageName;
    @Field(5)
    @PublicApi(exclude = true)
    public boolean androidInstallApp;
    @Field(6)
    @PublicApi(exclude = true)
    public String androidMinimumVersion;
    @Field(7)
    @PublicApi(exclude = true)
    public boolean handleCodeInApp;
    @Field(8)
    @PublicApi(exclude = true)
    public String localeHeader;
    @Field(9)
    @PublicApi(exclude = true)
    public int requestType;
    @Field(10)
    @PublicApi(exclude = true)
    public String dynamicLinkDomain;

    private ActionCodeSettings() {
    }

    /**
     * @return whether the oob code should be handled by the app. See {@link Builder#setHandleCodeInApp(boolean)}
     */
    public boolean canHandleCodeInApp() {
        return handleCodeInApp;
    }

    /**
     * @return the preference for whether to attempt to install the app if it is not present. See {@link Builder#setAndroidPackageName(String, boolean, String)}
     */
    public boolean getAndroidInstallApp() {
        return androidInstallApp;
    }

    /**
     * @return the minimum Android app version. See {@link Builder#setAndroidPackageName(String, boolean, String)}
     */
    public String getAndroidMinimumVersion() {
        return androidMinimumVersion;
    }

    /**
     * @return the Android Package Name. See {@link Builder#setAndroidPackageName(String, boolean, String)}
     */
    public String getAndroidPackageName() {
        return androidPackageName;
    }

    /**
     * @return the iOS Bundle. See {@link Builder#setIOSBundleId(String)}
     */
    public String getIOSBundle() {
        return iOSBundle;
    }

    /**
     * @return the URL. See {@link Builder#setUrl(String)}
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return a new instance of {@link ActionCodeSettings.Builder}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A Builder class for {@link ActionCodeSettings}. Get an instance of this Builder using {@link #newBuilder()}.
     */
    public static class Builder {
        private String url;
        private String iOSBundleId;
        private String androidPackageName;
        private boolean androidInstallApp;
        private String androidMinimumVersion;
        private boolean canHandleCodeInApp;
        private String dynamicLinkDomain;

        public ActionCodeSettings build() {
            ActionCodeSettings settings = new ActionCodeSettings();
            settings.url = url;
            settings.iOSBundle = iOSBundleId;
            settings.androidPackageName = androidPackageName;
            settings.androidInstallApp = androidInstallApp;
            settings.handleCodeInApp = canHandleCodeInApp;
            settings.dynamicLinkDomain = dynamicLinkDomain;
            return settings;
        }

        /**
         * Sets the Android package name and returns the current builder instance.
         * If {@code installIfNotAvailable} is set to true and the link is opened on an android device, it will try to install the app if not already available.
         * Otherwise the web URL is used.
         * <p>
         * A minimum version string is also available. If the installed app is an older version, the user is taken to the Play Store to upgrade the app.
         */
        public Builder setAndroidPackageName(String androidPackageName, boolean installIfNotAvailable, String minimumVersion) {
            this.androidPackageName = androidPackageName;
            this.androidInstallApp = installIfNotAvailable;
            this.androidMinimumVersion = minimumVersion;
            return this;
        }

        /**
         * Sets the optional FDL domain, overriding the default FDL domain that would be used.
         * Must be one of the 5 domains configured in the Firebase console.
         */
        public Builder setDynamicLinkDomain(String dynamicLinkDomain) {
            this.dynamicLinkDomain = dynamicLinkDomain;
            return this;
        }

        /**
         * The default is false. When set to true, the action code link will be sent as a universal link and will be open by the app if installed.
         * In the false case, the code will be sent to the web widget first and then on continue will redirect to the app if installed.
         */
        public Builder setHandleCodeInApp(boolean status) {
            this.canHandleCodeInApp = status;
            return this;
        }

        /**
         * To be used if the email link that is sent might be opened on an iOS device.
         * <p>
         * Sets the iOS bundle Id and returns the current {@link ActionCodeSettings.Builder} instance.
         */
        public Builder setIOSBundleId(String iOSBundleId) {
            this.iOSBundleId = iOSBundleId;
            return this;
        }

        /**
         * Sets the URL, which has different meanings in different contexts. For email actions, this is the state/continue URL.
         * When the app is not installed, this is the web continue URL with any developer provided state appended (the continueURL query parameter).
         * When the app is installed, this is contained in the Firebase dynamic link payload.
         * In the case where the code is sent directly to the app and the app is installed, this is the continueURL query parameter in the dynamic link payload.
         * Otherwise, when the code is handled by the widget itself, it is the payload itself.
         */
        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }
    }

    public static final Creator<ActionCodeSettings> CREATOR = new AutoCreator<>(ActionCodeSettings.class);
}
