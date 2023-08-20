/*
 * SPDX-FileCopyrightText: 2016 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cast.framework;

import androidx.annotation.NonNull;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.LaunchOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration parameters for initializing the {@link CastContext}. The {@link CastOptions.Builder} is used to create an instance of
 * {@link CastOptions}, and so contains the corresponding setter methods.
 */
public class CastOptions extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    private String receiverApplicationId;
    @Field(3)
    private ArrayList<String> supportedNamespaces;
    @Field(4)
    private boolean stopReceiverApplicationWhenEndingSession;
    @Field(5)
    private LaunchOptions launchOptions;
    @Field(6)
    private boolean resumeSavedSession;
    @Field(7)
    private CastMediaOptions castMediaOptions;
    @Field(8)
    private boolean enableReconnectionService;
    @Field(9)
    private double volumeDeltaBeforeIceCreamSandwich;
    @Field(10)
    private boolean enableIpv6Support;
    @Field(11)
    private boolean outputSwitcherEnabled;
    @Field(12)
    private boolean isRemoteToLocalEnabled;
    @Field(13)
    private List<String> routeDiscoveryReceiverApplicationIds;
    @Field(14)
    private boolean sessionTransferEnabled;
    @Field(15)
    private int persistCastButtonEnabled;
    @Field(16)
    private boolean resumeSessionAfterTransferEnabled;

    /**
     * Returns the {@link CastMediaOptions} that is used to configure a media session.
     */
    public CastMediaOptions getCastMediaOptions() {
        return castMediaOptions;
    }

    /**
     * Returns {@code true} if {@link ReconnectionService} should be enabled when needed to better handle session recovery.
     */
    public boolean getEnableReconnectionService() {
        return enableReconnectionService;
    }

    /**
     * Returns the {@link LaunchOptions}.
     */
    public LaunchOptions getLaunchOptions() {
        return launchOptions;
    }

    /**
     * Returns the cast receiver application ID. This ID is used in discovering supported receivers, and launching an application
     * when starting a new session.
     */
    public String getReceiverApplicationId() {
        return receiverApplicationId;
    }

    /**
     * Returns {@code true} if the saved session should be resumed if it was dropped unexpectedly. Returns {@code false} if the saved
     * session should not be resumed in this case.
     */
    public boolean getResumeSavedSession() {
        return resumeSavedSession;
    }

    /**
     * Returns {@code true} if the receiver application should be stopped when the session is ended by the user. Returns {@code false} if the
     * receiver application should not be stopped in this case.
     */
    public boolean getStopReceiverApplicationWhenEndingSession() {
        return stopReceiverApplicationWhenEndingSession;
    }

    /**
     * Returns the list of supported cast namespaces.
     */
    @NonNull
    public List<String> getSupportedNamespaces() {
        return supportedNamespaces;
    }

    /**
     * Returns the amount of receiver device volume to increase or decrease when the physical volume key is pressed on an
     * Android device older than ICE CREAM SANDWICH. The SDK will call this method to obtain the volume delta when
     * {@link CastContext#onDispatchVolumeKeyEventBeforeJellyBean(KeyEvent)} is called. The return value is not used on
     * Android devices running ICE CREAM SANDWICH or newer.
     */
    @Deprecated
    public double getVolumeDeltaBeforeIceCreamSandwich() {
        return volumeDeltaBeforeIceCreamSandwich;
    }

    public static Creator<CastOptions> CREATOR = new AutoCreator<CastOptions>(CastOptions.class);
}
