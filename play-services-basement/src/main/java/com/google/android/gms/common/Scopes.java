/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common;

import org.microg.gms.common.Hide;

/**
 * OAuth 2.0 scopes for use with Google Play services. See the specific client methods for details on which scopes are required.
 */
public class Scopes {
    /**
     * OAuth 2.0 scope for viewing a user's basic profile information.
     */
    public static final String PROFILE = "profile";
    @Hide
    public static final String OPENID = "openid";
    /**
     * OAuth 2.0 scope for accessing user's Google account email address.
     */
    public static final String EMAIL = "email";
    /**
     * OAuth 2.0 scope for accessing the user's name, basic profile info and Google+ profile info.
     * <p>
     * When using this scope, your app will have access to:
     * <ul>
     * <li>the user's full name, profile picture, Google+ profile ID, age range, and language</li>
     * <li>any other publicly available information on the user's Google+ profile</li>
     * </ul>
     *
     * @deprecated We recommend switching to {@link #PROFILE} scope to get the one-tap sign-in experience. Your app will get much higher sign-in completion
     * rate by switching to profile scopes because of the streamlined user experience. And your existing users with PLUS_LOGIN grant will not be asked to
     * sign-in again.
     * If you really need user's age range and locale information (which is the only additional information you can get from PLUS_LOGIN as of
     * September 2016), use below scopes in addition to PROFILE:<ul>
     * <li>www.googleapis.com/auth/profile.agerange.read</li>
     * <li>www.googleapis.com/auth/profile.language.read</li>
     * </ul>
     */
    @Deprecated
    public static final String PLUS_LOGIN = "https://www.googleapis.com/auth/plus.login";
    /**
     * This scope was previously named PLUS_PROFILE.
     * <p>
     * When using this scope, it does the following:
     * <ul>
     * <li>It lets you know who the currently authenticated user is by letting you replace a Google+ user ID with "me", which represents the authenticated
     * user, in any call to the Google+ API.</li>
     * </ul>
     */
    public static final String PLUS_ME = "https://www.googleapis.com/auth/plus.me";
    /**
     * Scope for accessing data from Google Play Games.
     */
    public static final String GAMES = "https://www.googleapis.com/auth/games";
    @Hide
    public static final String GAMES_LITE = "https://www.googleapis.com/auth/games_lite";
    @Hide
    public static final String GAMES_FIRSTPARTY = "https://www.googleapis.com/auth/games.firstparty";
    /**
     * Scope for using the CloudSave service.
     */
    public static final String CLOUD_SAVE = "https://www.googleapis.com/auth/datastoremobile";
    /**
     * Scope for using the App State service.
     */
    public static final String APP_STATE = "https://www.googleapis.com/auth/appstate";
    /**
     * Scope for access user-authorized files from Google Drive.
     */
    public static final String DRIVE_FILE = "https://www.googleapis.com/auth/drive.file";
    /**
     * Scope for accessing appfolder files from Google Drive.
     */
    public static final String DRIVE_APPFOLDER = "https://www.googleapis.com/auth/drive.appdata";
    @Hide
    public static final String FITNESS_ACTIVITY_READ = "https://www.googleapis.com/auth/fitness.activity.read";
    @Hide
    public static final String FITNESS_ACTIVITY_READ_WRITE = "https://www.googleapis.com/auth/fitness.activity.write";
    @Hide
    public static final String FITNESS_LOCATION_READ = "https://www.googleapis.com/auth/fitness.location.read";
    @Hide
    public static final String FITNESS_LOCATION_READ_WRITE = "https://www.googleapis.com/auth/fitness.location.write";
    @Hide
    public static final String FITNESS_BODY_READ = "https://www.googleapis.com/auth/fitness.body.read";
    @Hide
    public static final String FITNESS_BODY_READ_WRITE = "https://www.googleapis.com/auth/fitness.body.write";
    @Hide
    public static final String USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email";
    @Hide
    public static final String USERINFO_PROFILE = "https://www.googleapis.com/auth/userinfo.profile";
    @Hide
    public static final String USER_BIRTHDAY_READ = "https://www.googleapis.com/auth/user.birthday.read";
    @Hide
    public static final String GMAIL_READONLY = "https://www.googleapis.com/auth/gmail.readonly";
    /**
     * Scope for cryptauthenrollment.googleapis.com (required for certain Google Workspace accounts)
     */
    @Hide
    public static final String CRYPTAUTH = "https://www.googleapis.com/auth/cryptauth";
}
