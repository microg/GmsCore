/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.cast;

import android.os.Bundle;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import org.microg.gms.cast.CastApiClientBuilder;
import org.microg.gms.cast.CastApiImpl;
import org.microg.gms.common.PublicApi;

import java.io.IOException;

@PublicApi
public final class Cast {

    /**
     * A constant indicating that the Google Cast device is not the currently active video input.
     */
    public static final int ACTIVE_INPUT_STATE_NO = 0;

    /**
     * A constant indicating that it is not known (and/or not possible to know) whether the Google Cast device is
     * the currently active video input. Active input state can only be reported when the Google Cast device is
     * connected to a TV or AVR with CEC support.
     */
    public static final int ACTIVE_INPUT_STATE_UNKNOWN = -1;

    /**
     * A constant indicating that the Google Cast device is the currently active video input.
     */
    public static final int ACTIVE_INPUT_STATE_YES = 1;

    /**
     * A boolean extra for the connection hint bundle passed to
     * {@link GoogleApiClient.ConnectionCallbacks#onConnected(Bundle)} that indicates that the connection was
     * re-established, but the receiver application that was in use at the time of the connection loss is no longer
     * running on the receiver.
     */
    public static final String EXTRA_APP_NO_LONGER_RUNNING = "com.google.android.gms.cast.EXTRA_APP_NO_LONGER_RUNNING";

    /**
     * The maximum raw message length (in bytes) that is supported by a Cast channel.
     */
    public static final int MAX_MESSAGE_LENGTH = 65536;

    /**
     * The maximum length (in characters) of a namespace name.
     */
    public static final int MAX_NAMESPACE_LENGTH = 128;

    /**
     * A constant indicating that the Google Cast device is not currently in standby.
     */
    public static final int STANDBY_STATE_NO = 0;

    /**
     * A constant indicating that it is not known (and/or not possible to know) whether the Google Cast device is
     * currently in standby. Standby state can only be reported when the Google Cast device is connected to a TV or
     * AVR with CEC support.
     */
    public static final int STANDBY_STATE_UNKNOWN = -1;

    /**
     * A constant indicating that the Google Cast device is currently in standby.
     */
    public static final int STANDBY_STATE_YES = 1;


    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable the Cast features.
     */
    public static final Api<CastOptions> API = new Api<CastOptions>(new CastApiClientBuilder());

    /**
     * An implementation of the CastApi interface. The interface is used to interact with a cast device.
     */
    public static final Cast.CastApi CastApi = new CastApiImpl();

    private Cast() {
    }

    public interface ApplicationConnectionResult extends Result {
        ApplicationMetadata getApplicationMetadata();

        String getApplicationStatus();

        String getSessionId();

        boolean getWasLaunched();
    }

    public interface CastApi {
        int getActiveInputState(GoogleApiClient client);

        ApplicationMetadata getApplicationMetadata(GoogleApiClient client);

        String getApplicationStatus(GoogleApiClient client);

        int getStandbyState(GoogleApiClient client);

        double getVolume(GoogleApiClient client);

        boolean isMute(GoogleApiClient client);

        PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient client);

        PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient client, String applicationId, String sessionId);

        PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient client, String applicationId);

        PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient client, String applicationId, LaunchOptions launchOptions);

        PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient client, String applicationId);

        @Deprecated
        PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient client, String applicationId, boolean relaunchIfRunning);

        PendingResult<Status> leaveApplication(GoogleApiClient client);

        void removeMessageReceivedCallbacks(GoogleApiClient client, String namespace) throws IOException;

        void requestStatus(GoogleApiClient client) throws IOException;

        PendingResult<Status> sendMessage(GoogleApiClient client, String namespace, String message);

        void setMessageReceivedCallbacks(GoogleApiClient client, String namespace, Cast.MessageReceivedCallback callbacks) throws IOException;

        void setMute(GoogleApiClient client, boolean mute) throws IOException;

        void setVolume(GoogleApiClient client, double volume) throws IOException;

        PendingResult<Status> stopApplication(GoogleApiClient client);

        PendingResult<Status> stopApplication(GoogleApiClient client, String sessionId);
    }

    public static class CastOptions implements Api.ApiOptions.HasOptions {
        private final CastDevice castDevice;
        private final Listener castListener;
        private final boolean verboseLoggingEnabled;

        public CastOptions(CastDevice castDevice, Listener castListener, boolean verboseLoggingEnabled) {
            this.castDevice = castDevice;
            this.castListener = castListener;
            this.verboseLoggingEnabled = verboseLoggingEnabled;
        }

        @Deprecated
        public static Builder builder(CastDevice castDevice, Listener castListener) {
            return new Builder(castDevice, castListener);
        }

        public static class Builder {
            private final CastDevice castDevice;
            private final Listener castListener;
            private boolean verboseLoggingEnabled;

            public Builder(CastDevice castDevice, Listener castListener) {
                this.castDevice = castDevice;
                this.castListener = castListener;
            }

            public CastOptions build() {
                return new CastOptions(castDevice, castListener, verboseLoggingEnabled);
            }

            public Builder setVerboseLoggingEnabled(boolean verboseLoggingEnabled) {
                this.verboseLoggingEnabled = verboseLoggingEnabled;
                return this;
            }
        }
    }

    public static class Listener {
        public void onActiveInputStateChanged(int activeInputState) {

        }

        public void onApplicationDisconnected(int statusCode) {

        }

        public void onApplicationMetadataChanged(ApplicationMetadata applicationMetadata) {

        }

        public void onApplicationStatusChanged() {

        }

        public void onStandbyStateChanged(int standbyState) {

        }

        public void onVolumeChanged() {

        }
    }

    public interface MessageReceivedCallback {
        void onMessageReceived(CastDevice castDevice, String namespace, String message);
    }
}
