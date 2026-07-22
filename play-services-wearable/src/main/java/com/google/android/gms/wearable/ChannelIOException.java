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

package com.google.android.gms.wearable;

import com.google.android.gms.wearable.ChannelApi.ChannelListener;

import java.io.IOException;

/**
 * A subclass of {@link IOException} which can be thrown from the streams returned by
 * {@link Channel#getInputStream(GoogleApiClient)} and {@link Channel#getOutputStream(GoogleApiClient)}.
 */
public class ChannelIOException extends IOException {

    private int closeReason;
    private int appSpecificErrorCode;

    public ChannelIOException(String message, int closeReason, int appSpecificErrorCode) {
        super(message);
        this.closeReason = closeReason;
        this.appSpecificErrorCode = appSpecificErrorCode;
    }

    /**
     * Returns the app-specific error code passed to {@link Channel#close(GoogleApiClient, int)} if
     * that's the reason for the stream closing, or {@code 0} otherwise.
     */
    public int getAppSpecificErrorCode() {
        return appSpecificErrorCode;
    }

    /**
     * Returns one of {@link ChannelListener#CLOSE_REASON_NORMAL}, {@link ChannelListener#CLOSE_REASON_DISCONNECTED},
     * {@link ChannelListener#CLOSE_REASON_REMOTE_CLOSE}, or {@link ChannelListener#CLOSE_REASON_LOCAL_CLOSE},
     * to indicate the reason for the stream closing.
     */
    public int getCloseReason() {
        return closeReason;
    }
}
