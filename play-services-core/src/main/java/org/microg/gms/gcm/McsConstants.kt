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
package org.microg.gms.gcm

object McsConstants {
    const val MCS_HEARTBEAT_PING_TAG = 0
    const val MCS_HEARTBEAT_ACK_TAG = 1
    const val MCS_LOGIN_REQUEST_TAG = 2
    const val MCS_LOGIN_RESPONSE_TAG = 3
    const val MCS_CLOSE_TAG = 4
    const val MCS_IQ_STANZA_TAG = 7
    const val MCS_DATA_MESSAGE_STANZA_TAG = 8
    const val MCS_VERSION_CODE = 41
    const val MSG_INPUT = 10
    const val MSG_INPUT_ERROR = 11
    const val MSG_OUTPUT = 20
    const val MSG_OUTPUT_ERROR = 21
    const val MSG_OUTPUT_READY = 22
    const val MSG_OUTPUT_DONE = 23
    const val MSG_TEARDOWN = 30
    const val MSG_CONNECT = 40
    const val MSG_HEARTBEAT = 41
    const val MSG_ACK = 42
    @JvmField
    var ACTION_CONNECT = "org.microg.gms.gcm.mcs.CONNECT"
    @JvmField
    var ACTION_RECONNECT = "org.microg.gms.gcm.mcs.RECONNECT"
    @JvmField
    var ACTION_HEARTBEAT = "org.microg.gms.gcm.mcs.HEARTBEAT"
    @JvmField
    var ACTION_SEND = "org.microg.gms.gcm.mcs.SEND"
    @JvmField
    var ACTION_ACK = "org.microg.gms.gcm.mcs.ACK"
    @JvmField
    var EXTRA_REASON = "org.microg.gms.gcm.mcs.REASON"
}