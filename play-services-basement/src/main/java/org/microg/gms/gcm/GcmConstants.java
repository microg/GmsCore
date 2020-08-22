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

public final class GcmConstants {
    public static final String ACTION_C2DM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
    public static final String ACTION_C2DM_REGISTER = "com.google.android.c2dm.intent.REGISTER";
    public static final String ACTION_C2DM_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";
    public static final String ACTION_C2DM_UNREGISTER = "com.google.android.c2dm.intent.UNREGISTER";
    public static final String ACTION_GCM_SEND = "com.google.android.gcm.intent.SEND";
    public static final String ACTION_NOTIFICATION_OPEN = "com.google.android.gms.gcm.NOTIFICATION_OPEN";
    public static final String ACTION_NOTIFICATION_DISMISS = "com.google.android.gms.gcm.NOTIFICATION_DISMISS";
    public static final String ACTION_SCHEDULE = "com.google.android.gms.gcm.ACTION_SCHEDULE";
    public static final String ACTION_TASK_READY = "com.google.android.gms.gcm.ACTION_TASK_READY";
    public static final String ACTION_TASK_INITIALZE = "com.google.android.gms.gcm.SERVICE_ACTION_INITIALIZE";
    public static final String ACTION_INSTANCE_ID = "com.google.android.gms.iid.InstanceID";

    public static final String EXTRA_APP = "app";
    public static final String EXTRA_APP_OVERRIDE = "org.microg.gms.gcm.APP_OVERRIDE";
    public static final String EXTRA_APP_ID = "appid";
    public static final String EXTRA_APP_VERSION_CODE = "app_ver";
    public static final String EXTRA_APP_VERSION_NAME = "app_ver_name";
    public static final String EXTRA_CLIENT_VERSION = "cliv";
    public static final String EXTRA_COMPONENT = "component";
    public static final String EXTRA_COLLAPSE_KEY = "collapse_key";
    public static final String EXTRA_DELAY = "google.delay";
    public static final String EXTRA_DELETE = "delete";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_FROM = "from";
    public static final String EXTRA_GSF_INTENT = "GSF";
    public static final String EXTRA_GMS_VERSION = "gmsv";
    public static final String EXTRA_IS_MESSENGER2 = "messenger2";
    public static final String EXTRA_KID = "kid";
    public static final String EXTRA_MESSENGER = "google.messenger";
    public static final String EXTRA_MESSAGE_TYPE = "message_type";
    public static final String EXTRA_MESSAGE_ID = "google.message_id";
    public static final String EXTRA_OS_VERSION = "osv";
    public static final String EXTRA_PENDING_INTENT = "com.google.android.gms.gcm.PENDING_INTENT";
    public static final String EXTRA_PUBLIC_KEY = "pub2";
    public static final String EXTRA_RAWDATA = "rawData";
    public static final String EXTRA_RAWDATA_BASE64 = "gcm.rawData64";
    public static final String EXTRA_REGISTRATION_ID = "registration_id";
    public static final String EXTRA_RETRY_AFTER = "Retry-After";
    public static final String EXTRA_SCHEDULER_ACTION = "scheduler_action";
    public static final String EXTRA_SCOPE = "scope";
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_SENDER_LEGACY = "legacy.sender";
    public static final String EXTRA_SEND_TO = "google.to";
    public static final String EXTRA_SEND_FROM = "google.from";
    public static final String EXTRA_SIGNATURE = "sig";
    public static final String EXTRA_SUBSCIPTION = "subscription";
    public static final String EXTRA_SUBTYPE = "subtype";
    public static final String EXTRA_USE_GSF = "useGsf";
    public static final String EXTRA_TAG = "tag";
    public static final String EXTRA_TOPIC = "gcm.topic";
    public static final String EXTRA_TTL = "google.ttl";
    public static final String EXTRA_UNREGISTERED = "unregistered";

    public static final String MESSAGE_TYPE_GCM = "gcm";
    public static final String MESSAGE_TYPE_DELETED_MESSAGE = "deleted_message";
    public static final String MESSAGE_TYPE_SEND_ERROR = "send_error";
    public static final String MESSAGE_TYPE_SEND_EVENT = "send_event";

    public static final String SCHEDULER_ACTION_CANCEL = "CANCEL_TASK";
    public static final String SCHEDULER_ACTION_CANCEL_ALL = "CANCEL_ALL";
    public static final String SCHEDULER_ACTION_SCHEDULE = "SCHEDULE_TASK";

    public static final String PERMISSION_GTALK = "com.google.android.gtalkservice.permission.GTALK_SERVICE";
    public static final String PERMISSION_NETWORK_TASK = "com.google.android.gms.permission.BIND_NETWORK_TASK_SERVICE";
    public static final String PERMISSION_RECEIVE = "com.google.android.c2dm.permission.RECEIVE";
    public static final String PERMISSION_SEND = "com.google.android.c2dm.permission.SEND";

    public static final String ERROR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";

    public static final String INSTANCE_ID_SCOPE_GCM = "GCM";

    public static final String GCMID_INSTANCE_ID = "google.com/iid";
    public static final String GCMID_REFRESH = "gcm.googleapis.com/refresh";
}
