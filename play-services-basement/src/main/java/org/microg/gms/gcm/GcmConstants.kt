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

object GcmConstants {
    const val ACTION_C2DM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE"
    const val ACTION_C2DM_REGISTER = "com.google.android.c2dm.intent.REGISTER"
    const val ACTION_C2DM_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION"
    const val ACTION_C2DM_UNREGISTER = "com.google.android.c2dm.intent.UNREGISTER"
    const val ACTION_GCM_SEND = "com.google.android.gcm.intent.SEND"
    const val ACTION_NOTIFICATION_OPEN = "com.google.android.gms.gcm.NOTIFICATION_OPEN"
    const val ACTION_NOTIFICATION_DISMISS = "com.google.android.gms.gcm.NOTIFICATION_DISMISS"
    const val ACTION_SCHEDULE = "com.google.android.gms.gcm.ACTION_SCHEDULE"
    const val ACTION_TASK_READY = "com.google.android.gms.gcm.ACTION_TASK_READY"
    const val ACTION_TASK_INITIALZE = "com.google.android.gms.gcm.SERVICE_ACTION_INITIALIZE"
    const val ACTION_INSTANCE_ID = "com.google.android.gms.iid.InstanceID"
    const val EXTRA_APP = "app"
    const val EXTRA_APP_OVERRIDE = "org.microg.gms.gcm.APP_OVERRIDE"
    const val EXTRA_APP_ID = "appid"
    const val EXTRA_APP_VERSION_CODE = "app_ver"
    const val EXTRA_APP_VERSION_NAME = "app_ver_name"
    const val EXTRA_CLIENT_VERSION = "cliv"
    const val EXTRA_COMPONENT = "component"
    const val EXTRA_COLLAPSE_KEY = "collapse_key"
    const val EXTRA_DELAY = "google.delay"
    const val EXTRA_DELETE = "delete"
    const val EXTRA_ERROR = "error"
    const val EXTRA_FROM = "from"
    const val EXTRA_GSF_INTENT = "GSF"
    const val EXTRA_GMS_VERSION = "gmsv"
    const val EXTRA_IS_MESSENGER2 = "messenger2"
    const val EXTRA_KID = "kid"
    const val EXTRA_MESSENGER = "google.messenger"
    const val EXTRA_MESSAGE_TYPE = "message_type"
    const val EXTRA_MESSAGE_ID = "google.message_id"
    const val EXTRA_OS_VERSION = "osv"
    const val EXTRA_PENDING_INTENT = "com.google.android.gms.gcm.PENDING_INTENT"
    const val EXTRA_PUBLIC_KEY = "pub2"
    const val EXTRA_RAWDATA = "rawData"
    const val EXTRA_RAWDATA_BASE64 = "gcm.rawData64"
    const val EXTRA_REGISTRATION_ID = "registration_id"
    const val EXTRA_RETRY_AFTER = "Retry-After"
    const val EXTRA_SCHEDULER_ACTION = "scheduler_action"
    const val EXTRA_SCOPE = "scope"
    const val EXTRA_SENDER = "sender"
    const val EXTRA_SENDER_LEGACY = "legacy.sender"
    const val EXTRA_SEND_TO = "google.to"
    const val EXTRA_SEND_FROM = "google.from"
    const val EXTRA_SIGNATURE = "sig"
    const val EXTRA_SUBSCIPTION = "subscription"
    const val EXTRA_SUBTYPE = "subtype"
    const val EXTRA_USE_GSF = "useGsf"
    const val EXTRA_TAG = "tag"
    const val EXTRA_TOPIC = "gcm.topic"
    const val EXTRA_TTL = "google.ttl"
    const val EXTRA_UNREGISTERED = "unregistered"
    const val MESSAGE_TYPE_GCM = "gcm"
    const val MESSAGE_TYPE_DELETED_MESSAGE = "deleted_message"
    const val MESSAGE_TYPE_SEND_ERROR = "send_error"
    const val MESSAGE_TYPE_SEND_EVENT = "send_event"
    const val SCHEDULER_ACTION_CANCEL = "CANCEL_TASK"
    const val SCHEDULER_ACTION_CANCEL_ALL = "CANCEL_ALL"
    const val SCHEDULER_ACTION_SCHEDULE = "SCHEDULE_TASK"
    const val PERMISSION_GTALK = "com.google.android.gtalkservice.permission.GTALK_SERVICE"
    const val PERMISSION_NETWORK_TASK = "com.google.android.gms.permission.BIND_NETWORK_TASK_SERVICE"
    const val PERMISSION_RECEIVE = "com.google.android.c2dm.permission.RECEIVE"
    const val PERMISSION_SEND = "com.google.android.c2dm.permission.SEND"
    const val ERROR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE"
    const val INSTANCE_ID_SCOPE_GCM = "GCM"
    const val GCMID_INSTANCE_ID = "google.com/iid"
    const val GCMID_REFRESH = "gcm.googleapis.com/refresh"
}