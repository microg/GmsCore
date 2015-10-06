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

package org.microg.gms.common;

import java.lang.reflect.Field;

public final class Services {

    private Services() {
    }

    public interface GAMES {
        int SERVICE_ID = 1;
        String ACTION = "com.google.android.gms.games.service.START";
    }

    public interface PLUS {
        int SERVICE_ID = 2;
        String ACTION = "com.google.android.gms.plus.service.START";
        String ACTION_INTERNAL = "com.google.android.gms.plus.service.internal.START";
    }

    public interface PANORAMA {
        int SERVICE_ID = 3;
        String ACTION = "com.google.android.gms.panorama.service.START";
    }

    public interface WALLET {
        int SERVICE_ID = 4;
        String ACTION = "com.google.android.gms.wallet.service.BIND";
    }

    public interface PEOPLE {
        int SERVICE_ID = 5;
        String ACTION = "com.google.android.gms.people.service.START";
    }

    public interface LOCATION {
        int SERVICE_ID = 6;
    }

    public interface APPSTATE {
        int SERVICE_ID = 7;
        String ACTION = "com.google.android.gms.appstate.service.START";
    }

    public interface ADREQUEST {
        int SERVICE_ID = 8;
        String ACTION = "com.google.android.gms.ads.service.START";
    }

    public interface ACCOUNT {
        int SERVICE_ID = 9;
        String ACTION = "com.google.android.gms.accounts.ACCOUNT_SERVICE";
    }

    public interface CAST {
        int SERVICE_ID = 10;
        String ACTION = "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE";
    }

    public interface DRIVE {
        int SERVICE_ID = 11;
        String ACTION = "com.google.android.gms.drive.ApiService.START";
    }

    public interface ADDRESS {
        int SERVICE_ID = 12;
        String ACTION = "com.google.android.gms.identity.service.BIND";
    }

    public interface CAR {
        int SERVICE_ID = 13;
        String ACTION = "com.google.android.gms.car.service.START";
    }

    public interface WEARABLE {
        int SERVICE_ID = 14;
        String ACTION = "com.google.android.gms.wearable.BIND";
    }

    public interface AUTH {
        int SERVICE_ID = 16;
        String ACTION = "com.google.android.gms.auth.service.START";
    }

    public interface FITNESS {
        int SERVICE_ID = 17;
        String ACTION = "com.google.android.gms.fitness.GoogleFitnessService.START";
    }

    public interface REMINDERS {
        int SERVICE_ID = 18;
        String ACTION = "com.google.android.gms.reminders.service.START";
    }

    public interface LIGHTWEIGHT_INDEX {
        int SERVICE_ID = 19;
        String ACTION = "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE";
    }

    public interface DEVICE_CONNECTION {
        int SERVICE_ID = 20;
        String ACTION = "com.google.android.gms.deviceconnection.service.START";
    }

    public interface INDEX {
        int SERVICE_ID = 21;
        String ACTION = "com.google.android.gms.icing.INDEX_SERVICE";
    }

    public interface LOCATION_REPORTING {
        int SERVICE_ID = 22;
        String ACTION = "com.google.android.gms.location.reporting.service.START";
        String ACTION_LEGACY = "com.google.android.location.reporting.service.START";
    }

    public interface LOCATION_MANAGER {
        int SERVICE_ID = 23;
        String ACTION_LEGACY = "com.google.android.location.internal.GoogleLocationManagerService.START";
    }

    public interface PLAY_LOG {
        int SERVICE_ID = 24;
        String ACTION = "com.google.android.gms.playlog.service.START";
    }

    public interface DROIDGUARD {
        int SERVICE_ID = 25;
        String ACTION = "com.google.android.gms.droidguard.service.START";
    }

    public interface LOCKBOX {
        int SERVICE_ID = 26;
        String ACTION = "com.google.android.gms.lockbox.service.START";
    }

    public interface CAST_MIRRORING {
        int SERVICE_ID = 27;
        String ACTION = "com.google.android.gms.cast_mirroring.service.START";
    }

    public interface NETWORK_QUALITY {
        int SERVICE_ID = 28;
        String ACTION = "com.google.android.gms.mdm.services.START";
    }

    public interface FEEDBACK {
        int SERVICE_ID = 29;
        String ACTION = "com.google.android.gms.feedback.internal.IFeedbackService";
    }

    public interface SEARCH_ADMINISTRATION {
        int SERVICE_ID = 30;
    }

    public interface PHOTO_AUTO_BACKUP {
        int SERVICE_ID = 31;
        String ACTION = "com.google.android.gms.photos.autobackup.service.START";
    }

    public interface SEARCH_QUERIES {
        int SERVICE_ID = 32;
    }

    public interface SEARCH_GLOBAL {
        int SERVICE_ID = 33;
    }

    public interface UDC {
        int SERVICE_ID = 35;
        String ACTION = "com.google.android.gms.udc.service.START";
    }

    public interface SEARCH_CORPORA {
        int SERVICE_ID = 36;
    }

    public interface DEVICE_MANAGER {
        int SERVICE_ID = 37;
        String ACTION = "com.google.android.gms.mdm.services.DeviceManagerApiService.START";
    }

    public interface PSEUDONYMOUS_ID {
        int SERVICE_ID = 38;
        String ACTION = "com.google.android.gms.pseudonymous.service.START";
    }

    public interface COMMON {
        int SERVICE_ID = 39;
        String ACTION = "com.google.android.gms.common.service.START";
    }

    public interface CLEARCUT_LOGGER {
        int SERVICE_ID = 40;
        String ACTION = "com.google.android.gms.clearcut.service.START";
    }

    public interface USAGE_REPORTING {
        int SERVICE_ID = 41;
        String ACTION = "com.google.android.gms.usagereporting.service.START";
    }

    public interface KIDS {
        int SERVICE_ID = 42;
        String ACTION = "com.google.android.gms.kids.service.START";
    }

    public interface DOWNLOAD {
        int SERVICE_ID = 43;
        String ACTION = "com.google.android.gms.common.download.START";
    }

    public interface SIGN_IN {
        int SERVICE_ID = 44;
        String ACTION = "com.google.android.gms.signin.service.START";
    }

    public interface SAFETY_NET_CLIENT {
        int SERVICE_ID = 45;
        String ACTION = "com.google.android.gms.safetynet.service.START";
    }

    public interface GSERVICES {
        int SERVICE_ID = 46;
        String ACTION = "com.google.android.gms.ads.gservice.START";
    }

    public interface CONTEXT_MANAGER {
        int SERVICE_ID = 47;
        String ACTION = "com.google.android.contextmanager.service.ContextManagerService.START";
    }

    public interface AUDIO_MODEM {
        int SERVICE_ID = 48;
        String ACTION = "com.google.android.gms.audiomodem.service.AudioModemService.START";
    }

    public interface NEARBY_SHARING {
        int SERVICE_ID = 49;
        String ACTION = "com.google.android.gms.nearby.sharing.service.NearbySharingService.START";
    }

    public interface LIGHTWEIGHT_NETWORK_QUALITY {
        int SERVICE_ID = 51;
        String ACTION = "com.google.android.gms.herrevad.services.LightweightNetworkQualityAndroidService.START";
    }

    public interface PHENOTYPE {
        int SERVICE_ID = 51;
        String ACTION = "com.google.android.gms.phenotype.service.START";
    }

    public interface VOICE_UNLOCK {
        int SERVICE_ID = 52;
        String ACTION = "com.google.android.gms.speech.service.START";
    }

    public interface NEARBY_CONNECTIONS {
        int SERVICE_ID = 54;
        String ACTION = "com.google.android.gms.nearby.connection.service.START";
    }

    public interface FITNESS_SENSORS {
        int SERVICE_ID = 55;
        String ACTION = "com.google.android.gms.fitness.SensorsApi";
    }

    public interface FITNESS_RECORDING {
        int SERVICE_ID = 56;
        String ACTION = "com.google.android.gms.fitness.RecordingApi";
    }

    public interface FITNESS_HISTORY {
        int SERVICE_ID = 57;
        String ACTION = "com.google.android.gms.fitness.HistoryApi";
    }

    public interface FITNESS_SESSIONS {
        int SERVICE_ID = 58;
        String ACTION = "com.google.android.gms.fitness.SessionsApi";
    }

    /**
     * BLE = Bluetooth Low Energy
     */
    public interface FITNESS_BLE {
        int SERVICE_ID = 59;
        String ACTION = "com.google.android.gms.fitness.BleApi";
    }

    public interface FITNESS_CONFIG {
        int SERVICE_ID = 60;
        String ACTION = "com.google.android.gms.fitness.ConfigApi";
    }

    public interface FITNESS_INTERNAL {
        int SERVICE_ID = 61;
        String ACTION = "com.google.android.gms.fitness.InternalApi";
    }

    public interface NEARBY_MESSAGES {
        int SERVICE_ID = 62;
        String ACTION = "com.google.android.gms.nearby.messages.service.NearbyMessagesService.START";
    }

    public interface HELP {
        int SERVICE_ID = 63;
        String ACTION = "com.google.android.gms.googlehelp.service.GoogleHelpService.START";
    }

    public interface CONFIG {
        int SERVICE_ID = 64;
        String ACTION = "com.google.android.gms.config.START";
    }

    public interface GEODATA {
        int SERVICE_ID = 65;
    }

    public interface SEARCH_IME {
        int SERVICE_ID = 66;
    }

    public interface PLACE_DETECTION {
        int SERVICE_ID = 67;
    }

    public interface CREDENTIALS {
        int SERVICE_ID = 68;
        String ACTION = "com.google.android.gms.auth.api.credentials.service.START";
    }

    public interface ADVERTISING_ID {
        // Has no service id
        String ACTION = "com.google.android.gms.ads.identifier.service.START";
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
