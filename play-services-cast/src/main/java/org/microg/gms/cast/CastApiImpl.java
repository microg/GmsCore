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

package org.microg.gms.cast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

// TODO
public class CastApiImpl implements Cast.CastApi {
    @Override
    public int getActiveInputState(GoogleApiClient client) {
        return 0;
    }

    @Override
    public ApplicationMetadata getApplicationMetadata(GoogleApiClient client) {
        return null;
    }

    @Override
    public String getApplicationStatus(GoogleApiClient client) {
        return null;
    }

    @Override
    public int getStandbyState(GoogleApiClient client) {
        return 0;
    }

    @Override
    public double getVolume(GoogleApiClient client) {
        return 0;
    }

    @Override
    public boolean isMute(GoogleApiClient client) {
        return false;
    }

    @Override
    public PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient client) {
        return null;
    }

    @Override
    public PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient client, String applicationId, String sessionId) {
        return null;
    }

    @Override
    public PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient client, String applicationId) {
        return null;
    }

    @Override
    public PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient client, String applicationId, LaunchOptions launchOptions) {
        return null;
    }

    @Override
    public PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient client, String applicationId) {
        return null;
    }

    @Override
    public PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient client, String applicationId, boolean relaunchIfRunning) {
        return null;
    }

    @Override
    public PendingResult<Status> leaveApplication(GoogleApiClient client) {
        return null;
    }

    @Override
    public void removeMessageReceivedCallbacks(GoogleApiClient client, String namespace) throws IOException {

    }

    @Override
    public void requestStatus(GoogleApiClient client) throws IOException {

    }

    @Override
    public PendingResult<Status> sendMessage(GoogleApiClient client, String namespace, String message) {
        return null;
    }

    @Override
    public void setMessageReceivedCallbacks(GoogleApiClient client, String namespace, Cast.MessageReceivedCallback callbacks) throws IOException {

    }

    @Override
    public void setMute(GoogleApiClient client, boolean mute) throws IOException {

    }

    @Override
    public void setVolume(GoogleApiClient client, double volume) throws IOException {

    }

    @Override
    public PendingResult<Status> stopApplication(GoogleApiClient client) {
        return null;
    }

    @Override
    public PendingResult<Status> stopApplication(GoogleApiClient client, String sessionId) {
        return null;
    }
}
