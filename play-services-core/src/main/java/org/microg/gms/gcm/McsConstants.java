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

package org.microg.gms.gcm;

public final class McsConstants {
    public static final int MCS_HEARTBEAT_PING_TAG = 0;
    public static final int MCS_HEARTBEAT_ACK_TAG = 1;
    public static final int MCS_LOGIN_REQUEST_TAG = 2;
    public static final int MCS_LOGIN_RESPONSE_TAG = 3;
    public static final int MCS_CLOSE_TAG = 4;
    public static final int MCS_IQ_STANZA_TAG = 7;
    public static final int MCS_DATA_MESSAGE_STANZA_TAG = 8;

    public static final int MCS_VERSION_CODE = 41;

    public static final int MSG_INPUT = 10;
    public static final int MSG_INPUT_ERROR = 11;
    public static final int MSG_OUTPUT = 20;
    public static final int MSG_OUTPUT_ERROR = 21;
    public static final int MSG_OUTPUT_READY = 22;
    public static final int MSG_OUTPUT_DONE = 23;
    public static final int MSG_TEARDOWN = 30;
    public static final int MSG_CONNECT = 40;
    public static final int MSG_HEARTBEAT = 41;
    public static final int MSG_ACK = 42;

    public static String ACTION_CONNECT = "org.microg.gms.gcm.mcs.CONNECT";
    public static String ACTION_RECONNECT = "org.microg.gms.gcm.mcs.RECONNECT";
    public static String ACTION_HEARTBEAT = "org.microg.gms.gcm.mcs.HEARTBEAT";
    public static String ACTION_SEND = "org.microg.gms.gcm.mcs.SEND";
    public static String ACTION_ACK = "org.microg.gms.gcm.mcs.ACK";
    public static String EXTRA_REASON = "org.microg.gms.gcm.mcs.REASON";
}
