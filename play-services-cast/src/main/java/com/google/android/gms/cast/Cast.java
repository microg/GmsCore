/*
 * Copyright 2013-2015 microG Project Team
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

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

public class Cast {

    public static final Api<CastOptions> API = new Api<CastOptions>(null); // TODO
    public static final Cast.CastApi CastApi = null; // TODO

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
