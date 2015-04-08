/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.common;

import java.lang.reflect.Field;

public final class Services {
    public static final class GAMES {
        public static final int SERVICE_ID = 1;
        public static final String ACTION = "com.google.android.gms.games.service.START";
    }

    public static final class PLUS {
        public static final int SERVICE_ID = 2;
        public static final String ACTION = "com.google.android.gms.plus.service.START";
        public static final String ACTION_INTERNAL = "com.google.android.gms.plus.service.internal.START";
    }

    public static final class PANORAMA {
        public static final int SERVICE_ID = 3;
        public static final String ACTION = "com.google.android.gms.panorama.service.START";
    }

    public static final class WALLET {
        public static final int SERVICE_ID = 4;
        public static final String ACTION = "com.google.android.gms.wallet.service.BIND";
    }

    public static final class PEOPLE {
        public static final int SERVICE_ID = 5;
        public static final String ACTION = "com.google.android.gms.people.service.START";
    }

    public static final class LOCATION {
        public static final int SERVICE_ID = 6;
    }

    public static final class APPSTATE {
        public static final int SERVICE_ID = 7;
        public static final String ACTION = "com.google.android.gms.appstate.service.START";
    }

    public static final class ADREQUEST {
        public static final int SERVICE_ID = 8;
        public static final String ACTION = "com.google.android.gms.ads.service.START";
    }

    public static final class ACCOUNT {
        public static final int SERVICE_ID = 9;
        public static final String ACTION = "com.google.android.gms.accounts.ACCOUNT_SERVICE";
    }

    public static final class CAST {
        public static final int SERVICE_ID = 10;
        public static final String ACTION = "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE";
    }

    public static final class DRIVE {
        public static final int SERVICE_ID = 11;
        public static final String ACTION = "com.google.android.gms.drive.ApiService.START";
    }

    public static final class ADDRESS {
        public static final int SERVICE_ID = 12;
        public static final String ACTION = "com.google.android.gms.identity.service.BIND";
    }

    public static final class CAR {
        public static final int SERVICE_ID = 13;
        public static final String ACTION = "com.google.android.gms.car.service.START";
    }

    public static final class WEARABLE {
        public static final int SERVICE_ID = 14;
        public static final String ACTION = "com.google.android.gms.wearable.BIND";
    }

    public static final class AUTH {
        public static final int SERVICE_ID = 16;
        public static final String ACTION = "com.google.android.gms.auth.service.START";
    }

    public static final class FITNESS {
        public static final int SERVICE_ID = 17;
        public static final String ACTION = "com.google.android.gms.fitness.GoogleFitnessService.START";
    }

    public static final class REMINDERS {
        public static final int SERVICE_ID = 18;
        public static final String ACTION = "com.google.android.gms.reminders.service.START";
    }

    public static final class LIGHTWEIGHT_INDEX {
        public static final int SERVICE_ID = 19;
        public static final String ACTION = "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE";
    }

    public static final class DEVICE_CONNECTION {
        public static final int SERVICE_ID = 20;
        public static final String ACTION = "com.google.android.gms.deviceconnection.service.START";
    }

    public static final class INDEX {
        public static final int SERVICE_ID = 21;
        public static final String ACTION = "com.google.android.gms.icing.INDEX_SERVICE";
    }

    public static final class LOCATION_REPORTING {
        public static final int SERVICE_ID = 22;
        public static final String ACTION = "com.google.android.gms.location.reporting.service.START";
    }

    public static final class LOCATION_MANAGER {
        public static final int SERVICE_ID = 23;
        public static final String ACTION = "com.google.android.location.internal.GoogleLocationManagerService.START";
    }

    public static final class PLAY_LOG {
        public static final int SERVICE_ID = 24;
    }

    public static final class DROIDGUARD {
        public static final int SERVICE_ID = 25;
        public static final String ACTION = "com.google.android.gms.droidguard.service.START";
    }

    public static final class LOCKBOX {
        public static final int SERVICE_ID = 26;
        public static final String ACTION = "com.google.android.gms.lockbox.service.START";
    }

    public static final class CAST_MIRRORING {
        public static final int SERVICE_ID = 27;
        public static final String ACTION = "com.google.android.gms.cast_mirroring.service.START";
    }

    public static final class NETWORK_QUALITY {
        public static final int SERVICE_ID = 28;
        public static final String ACTION = "com.google.android.gms.mdm.services.START";
    }

    public static final class FEEDBACK {
        public static final int SERVICE_ID = 29;
        public static final String ACTION = "com.google.android.gms.feedback.internal.IFeedbackService";
    }

    public static final class SEARCH_ADMINISTRATION {
        public static final int SERVICE_ID = 30;
    }

    public static final class PHOTO_AUTO_BACKUP {
        public static final int SERVICE_ID = 31;
        public static final String ACTION = "com.google.android.gms.photos.autobackup.service.START";
    }

    public static final class SEARCH_QUERIES {
        public static final int SERVICE_ID = 32;
    }

    public static final class SEARCH_GLOBAL {
        public static final int SERVICE_ID = 33;
    }

    public static final class UDC {
        public static final int SERVICE_ID = 35;
        public static final String ACTION = "com.google.android.gms.udc.service.START";
    }

    public static final class SEARCH_CORPORA {
        public static final int SERVICE_ID = 36;
    }

    public static final class DEVICE_MANAGER {
        public static final int SERVICE_ID = 37;
        public static final String ACTION = "com.google.android.gms.mdm.services.DeviceManagerApiService.START";
    }

    public static final class PSEUDONYMOUS_ID {
        public static final int SERVICE_ID = 38;
        public static final String ACTION = "com.google.android.gms.pseudonymous.service.START";
    }

    public static final class COMMON {
        public static final int SERVICE_ID = 39;
        public static final String ACTION = "com.google.android.gms.common.service.START";
    }

    public static final class CLEARCUT_LOGGER {
        public static final int SERVICE_ID = 40;
        public static final String ACTION = "com.google.android.gms.clearcut.service.START";
    }

    public static final class USAGE_REPORTING {
        public static final int SERVICE_ID = 41;
        public static final String ACTION = "com.google.android.gms.usagereporting.service.START";
    }

    public static final class KIDS {
        public static final int SERVICE_ID = 42;
        public static final String ACTION = "com.google.android.gms.kids.service.START";
    }

    public static final class DOWNLOAD {
        public static final int SERVICE_ID = 43;
        public static final String ACTION = "com.google.android.gms.common.download.START";
    }

    public static final class SIGN_IN {
        public static final int SERVICE_ID = 44;
        public static final String ACTION = "com.google.android.gms.signin.service.START";
    }

    public static final class SAFETY_NET_CLIENT {
        public static final int SERVICE_ID = 45;
        public static final String ACTION = "com.google.android.gms.safetynet.service.START";
    }

    public static final class GSERVICES {
        public static final int SERVICE_ID = 46;
        public static final String ACTION = "com.google.android.gms.ads.gservice.START";
    }

    public static final class CONTEXT_MANAGER {
        public static final int SERVICE_ID = 47;
        public static final String ACTION = "com.google.android.contextmanager.service.ContextManagerService.START";
    }

    public static final class AUDIO_MODEM {
        public static final int SERVICE_ID = 48;
        public static final String ACTION = "com.google.android.gms.audiomodem.service.AudioModemService.START";
    }

    public static final class NEARBY_SHARING {
        public static final int SERVICE_ID = 49;
        public static final String ACTION = "com.google.android.gms.nearby.sharing.service.NearbySharingService.START";
    }

    public static final class LIGHTWEIGHT_NETWORK_QUALITY {
        public static final int SERVICE_ID = 51;
        public static final String ACTION = "com.google.android.gms.herrevad.services.LightweightNetworkQualityAndroidService.START";
    }

    public static final class PHENOTYPE {
        public static final int SERVICE_ID = 51;
        public static final String ACTION = "com.google.android.gms.phenotype.service.START";
    }

    public static final class VOICE_UNLOCK {
        public static final int SERVICE_ID = 52;
        public static final String ACTION = "com.google.android.gms.speech.service.START";
    }

    public static final class NEARBY_CONNECTIONS {
        public static final int SERVICE_ID = 54;
        public static final String ACTION = "com.google.android.gms.nearby.connection.service.START";
    }

    public static final class FITNESS_SENSORS {
        public static final int SERVICE_ID = 55;
        public static final String ACTION = "com.google.android.gms.fitness.SensorsApi";
    }

    public static final class FITNESS_RECORDING {
        public static final int SERVICE_ID = 56;
        public static final String ACTION = "com.google.android.gms.fitness.RecordingApi";
    }

    public static final class FITNESS_HISTORY {
        public static final int SERVICE_ID = 57;
        public static final String ACTION = "com.google.android.gms.fitness.HistoryApi";
    }

    public static final class FITNESS_SESSIONS {
        public static final int SERVICE_ID = 58;
        public static final String ACTION = "com.google.android.gms.fitness.SessionsApi";
    }

    /**
     * BLE = Bluetooth Low Energy
     */
    public static final class FITNESS_BLE {
        public static final int SERVICE_ID = 59;
        public static final String ACTION = "com.google.android.gms.fitness.BleApi";
    }

    public static final class FITNESS_CONFIG {
        public static final int SERVICE_ID = 60;
        public static final String ACTION = "com.google.android.gms.fitness.ConfigApi";
    }

    public static final class FITNESS_INTERNAL {
        public static final int SERVICE_ID = 61;
        public static final String ACTION = "com.google.android.gms.fitness.InternalApi";
    }

    public static final class NEARBY_MESSAGES {
        public static final int SERVICE_ID = 62;
        public static final String ACTION = "com.google.android.gms.nearby.messages.service.NearbyMessagesService.START";
    }

    public static final class HELP {
        public static final int SERVICE_ID = 63;
        public static final String ACTION = "com.google.android.gms.googlehelp.service.GoogleHelpService.START";
    }

    public static final class CONFIG {
        public static final int SERVICE_ID = 64;
        public static final String ACTION = "com.google.android.gms.config.START";
    }

    public static final class GEODATA {
        public static final int SERVICE_ID = 65;
    }

    public static final class SEARCH_IME {
        public static final int SERVICE_ID = 66;
    }

    public static final class PLACE_DETECTION {
        public static final int SERVICE_ID = 67;
    }

    public static final class CREDENTIALS {
        public static final int SERVICE_ID = 68;
        public static final String ACTION = "com.google.android.gms.auth.api.credentials.service.START";
    }

    public static String nameFromServiceId(int serviceId) {
        for (Class cls : Services.class.getDeclaredClasses()) {
            try {
                Field serviceIdField = cls.getDeclaredField("SERVICE_ID");
                if ((Integer) serviceIdField.get(null) == serviceId) return cls.getSimpleName();
            } catch (Exception e) {
            }
        }
        return "UNKNOWN(" + serviceId + ")";
    }
}
