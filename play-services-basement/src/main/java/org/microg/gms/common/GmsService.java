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

package org.microg.gms.common;

public enum GmsService {
    UNKNOWN(-2),
    ANY(-1),
    GAMES(1, "com.google.android.gms.games.service.START"),
    PLUS(2, "com.google.android.gms.plus.service.START", "com.google.android.gms.plus.service.internal.START"),
    PANORAMA(3, "com.google.android.gms.panorama.service.START"),
    WALLET(4, "com.google.android.gms.wallet.service.BIND"),
    PEOPLE(5, "com.google.android.gms.people.service.START"),
    LOCATION(6),
    APPSTATE(7, "com.google.android.gms.appstate.service.START"),
    ADREQUEST(8, "com.google.android.gms.ads.service.START"),
    ACCOUNT(9, "com.google.android.gms.accounts.ACCOUNT_SERVICE"),
    CAST(10, "com.google.android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE"),
    DRIVE(11, "com.google.android.gms.drive.ApiService.START"),
    ADDRESS(12, "com.google.android.gms.identity.service.BIND"),
    CAR(13, "com.google.android.gms.car.service.START"),
    WEARABLE(14, "com.google.android.gms.wearable.BIND"),
    AUTH_PROXY(16, "com.google.android.gms.auth.service.START"),
    FITNESS(17, "com.google.android.gms.fitness.GoogleFitnessService.START"),
    REMINDERS(18, "com.google.android.gms.reminders.service.START"),
    LIGHTWEIGHT_INDEX(19, "com.google.android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE"),
    DEVICE_CONNECTION(20, "com.google.android.gms.deviceconnection.service.START"),
    INDEX(21, "com.google.android.gms.icing.INDEX_SERVICE"),
    LOCATION_REPORTING(22, "com.google.android.gms.location.reporting.service.START", "com.google.android.location.reporting.service.START"),
    LOCATION_MANAGER(23, "com.google.android.location.internal.GoogleLocationManagerService.START"),
    PLAY_LOG(24, "com.google.android.gms.playlog.service.START"),
    DROIDGUARD(25, "com.google.android.gms.droidguard.service.START"),
    LOCKBOX(26, "com.google.android.gms.lockbox.service.START"),
    CAST_MIRRORING(27, "com.google.android.gms.cast_mirroring.service.START"),
    NETWORK_QUALITY(28, "com.google.android.gms.mdm.services.START"),
    FEEDBACK(29, "com.google.android.gms.feedback.internal.IFeedbackService"),
    SEARCH_ADMINISTRATION(30),
    PHOTO_AUTO_BACKUP(31, "com.google.android.gms.photos.autobackup.service.START"),
    SEARCH_QUERIES(32),
    SEARCH_GLOBAL(33),
    UDC(35, "com.google.android.gms.udc.service.START"),
    SEARCH_CORPORA(36),
    DEVICE_MANAGER(37, "com.google.android.gms.mdm.services.DeviceManagerApiService.START"),
    PSEUDONYMOUS_ID(38, "com.google.android.gms.pseudonymous.service.START"),
    COMMON(39, "com.google.android.gms.common.service.START"),
    CLEARCUT_LOGGER(40, "com.google.android.gms.clearcut.service.START"),
    USAGE_REPORTING(41, "com.google.android.gms.usagereporting.service.START"),
    KIDS(42, "com.google.android.gms.kids.service.START"),
    DOWNLOAD(43, "com.google.android.gms.common.download.START"),
    SIGN_IN(44, "com.google.android.gms.signin.service.START"),
    SAFETY_NET_CLIENT(45, "com.google.android.gms.safetynet.service.START"),
    GSERVICES(46, "com.google.android.gms.ads.gservice.START"),
    CONTEXT_MANAGER(47, "com.google.android.contextmanager.service.ContextManagerService.START"),
    AUDIO_MODEM(48, "com.google.android.gms.audiomodem.service.AudioModemService.START"),
    NEARBY_SHARING(49, "com.google.android.gms.nearby.sharing.service.NearbySharingService.START"),
    LIGHTWEIGHT_NETWORK_QUALITY(50, "com.google.android.gms.herrevad.services.LightweightNetworkQualityAndroidService.START"),
    PHENOTYPE(51, "com.google.android.gms.phenotype.service.START"),
    VOICE_UNLOCK(52, "com.google.android.gms.speech.service.START"),
    NEARBY_CONNECTIONS(54, "com.google.android.gms.nearby.connection.service.START"),
    FITNESS_SENSORS(55, "com.google.android.gms.fitness.SensorsApi"),
    FITNESS_RECORDING(56, "com.google.android.gms.fitness.RecordingApi"),
    FITNESS_HISTORY(57, "com.google.android.gms.fitness.HistoryApi"),
    FITNESS_SESSIONS(58, "com.google.android.gms.fitness.SessionsApi"),
    FITNESS_BLE(59, "com.google.android.gms.fitness.BleApi"),
    FITNESS_CONFIG(60, "com.google.android.gms.fitness.ConfigApi"),
    FITNESS_INTERNAL(61, "com.google.android.gms.fitness.InternalApi"),
    NEARBY_MESSAGES(62, "com.google.android.gms.nearby.messages.service.NearbyMessagesService.START"),
    HELP(63, "com.google.android.gms.googlehelp.service.GoogleHelpService.START"),
    CONFIG(64, "com.google.android.gms.config.START"),
    GEODATA(65, "com.google.android.gms.location.places.GeoDataApi"),
    SEARCH_IME(66),
    PLACE_DETECTION(67, "com.google.android.gms.location.places.PlaceDetectionApi"),
    CREDENTIALS(68, "com.google.android.gms.auth.api.credentials.service.START"),
    NEARBY_BOOTSTRAP(69, "com.google.android.gms.nearby.bootstrap.service.NearbyBootstrapService.START"),
    PLUS_INTERNAL(70),
    SOURCE_DEVICE(75, "com.google.android.gms.smartdevice.d2d.SourceDeviceService.START"),
    TARGET_DEVICE(76, "com.google.android.gms.smartdevice.d2d.TargetDeviceService.START"),
    APP_INVITE(77, "com.google.android.gms.appinvite.service.START"),
    TAP_AND_PAY(79, "com.google.android.gms.tapandpay.service.BIND"),
    CHROME_SYNC(80, "com.google.android.gms.chromesync.service.START"),
    ACCOUNTS(81, "com.google.android.gms.smartdevice.setup.accounts.AccountsService.START"),
    CAST_REMOTE_DISPLAY(83, "com.google.android.gms.cast.remote_display.service.START"),
    TRUST_AGENT(85, "com.google.android.gms.trustagent.StateApi.START"),
    AUTH_SIGN_IN(91, "com.google.android.gms.auth.api.signin.service.START"),
    MEASUREMENT(93, "com.google.android.gms.measurement.START"),
    FREIGHTER(98, "com.google.android.gms.freighter.service.START"),
    GUNS(110, "com.google.android.gms.notifications.service.START"),
    BLE(111, "com.google.android.gms.beacon.internal.IBleService.START"),
    FIREBASE_AUTH(112, "com.google.firebase.auth.api.gms.service.START"),
    APP_INDEXING(113),
    GASS(116, "com.google.android.gms.gass.START"),
    WORK_ACCOUNT(120),
    INSTANT_APPS(121, "com.google.android.gms.instantapps.START"),
    CAST_FIRSTPATY(122, "com.google.android.gms.cast.firstparty.START"),
    AD_CACHE(123, "com.google.android.gms.ads.service.CACHE"),
    SMS_RETRIEVER(126, "com.google.android.gms.auth.api.phone.service.SmsRetrieverApiService.START"),
    CRYPT_AUTH(129, "com.google.android.gms.auth.cryptauth.cryptauthservice.START"),
    DYNAMIC_LINKS(131, "com.google.firebase.dynamiclinks.service.START"),
    FONTS(132, "com.google.android.gms.fonts.service.START"),
    ROMANESCO(135, "com.google.android.gms.romanesco.service.START"),
    TRAINER(139, "com.google.android.gms.learning.trainer.START"),
    FIDO2_REGULAR(148, "com.google.android.gms.fido.fido2.regular.START"),
    FIDO2_PRIVILEGED(149, "com.google.android.gms.fido.fido2.privileged.START"),
    DATA_DOWNLOAD(152, "com.google.android.mdd.service.START"),
    ACCOUNT_DATA(153, "com.google.android.gms.auth.account.data.service.START"),
    CONSTELLATION(155, "com.google.android.gms.constellation.service.START"),
    AUDIT(154, "com.google.android.gms.audit.service.START"),
    SYSTEM_UPDATE(157, "com.google.android.gms.update.START_API_SERVICE"),
    USER_LOCATION(163, "com.google.android.gms.userlocation.service.START"),
    AD_HTTP(166, "com.google.android.gms.ads.service.HTTP"),
    LANGUAGE_PROFILE(167, "com.google.android.gms.languageprofile.service.START"),
    MDNS(168, "com.google.android.gms.mdns.service.START"),
    FIDO2_ZEROPARTY(180, "com.google.android.gms.fido.fido2.zeroparty.START"),
    G1_RESTORE(181, "com.google.android.gms.backup.G1_RESTORE"),
    G1_BACKUP(182, "com.google.android.gms.backup.G1_BACKUP"),
    OSS_LICENSES(185, "com.google.android.gms.oss.licenses.service.START"),
    PAYSE(188, "com.google.android.gms.payse.service.BIND"),
    RCS(189, "com.google.android.gms.rcs.START"),
    CARRIER_AUTH(191, "com.google.android.gms.carrierauth.service.START"),
    SYSTEM_UPDATE_SINGLE_UESR(192, "com.google.android.gms.update.START_SINGLE_USER_API_SERVICE"),
    APP_USAGE(193, "com.google.android.gms.appusage.service.START"),
    NEARBY_SHARING_2(194, "com.google.android.gms.nearby.sharing.START_SERVICE"),
    AD_CONSENT_LOOKUP(195, "com.google.android.gms.ads.service.CONSENT_LOOKUP"),
    PHONE_INTERNAL(197, "com.google.android.gms.auth.api.phone.service.InternalService.START"),
    PAY(198, "com.google.android.gms.pay.service.BIND"),
    ASTERISM(199, "com.google.android.gms.asterism.service.START"),
    MODULE_RESTORE(201, "com.google.android.gms.backup.GMS_MODULE_RESTORE"),
    FACS_CACHE(202, "com.google.android.gms.facs.cache.service.START"),
    RECAPTCHA(205, "com.google.android.gms.recaptcha.service.START"),
    CONTACT_SYNC(208, "com.google.android.gms.people.contactssync.service.START"),
    IDENTITY_SIGN_IN(212, "com.google.android.gms.auth.api.identity.service.signin.START"),
    CREDENTIAL_STORE(214, "com.google.android.gms.fido.credentialstore.internal_service.START"),
    EVENT_ATTESTATION(216, "com.google.android.gms.ads.identifier.service.EVENT_ATTESTATION"),
    SCHEDULER(218, "com.google.android.gms.scheduler.ACTION_PROXY_SCHEDULE"),
    AUTHORIZATION(219, "com.google.android.gms.auth.api.identity.service.authorization.START"),
    FACS_SYNC(220, "com.google.android.gms.facs.internal.service.START"),
    AUTH_CONFIG_SYNC(221, "com.google.android.gms.auth.config.service.START"),
    CREDENTIAL_SAVING(223, "com.google.android.gms.auth.api.identity.service.credentialsaving.START"),
    GOOGLE_AUTH(224, "com.google.android.gms.auth.account.authapi.START"),
    ENTERPRISE_LOADER(225, "com.google.android.gms.enterprise.loader.service.START"),
    THUNDERBIRD(226, "com.google.android.gms.thunderbird.service.START"),
    NEARBY_EXPOSURE(236, "com.google.android.gms.nearby.exposurenotification.START"),
    GMS_COMPLIANCE(257, "com.google.android.gms.gmscompliance.service.START"),
    FIDO_SOURCE_DEVICE(262, "com.google.android.gms.fido.sourcedevice.service.START"),
    FAST_PAIR(265, "com.google.android.gms.nearby.fastpair.START"),
    MATCHSTICK_LIGHTER(268, "com.google.android.gms.matchstick.lighter.service.START"),
    FIDO_TARGET_DEVICE_INTERNAL(269, "com.google.android.gms.fido.targetdevice.internal_service.START"),
    TELEMETRY(270, "com.google.android.gms.common.telemetry.service.START"),
    SECOND_DEVICE_AUTH(275, "com.google.android.gms.setup.auth.SecondDeviceAuth.START"),
    LOCATION_SHARING_REPORTER(277, "com.google.android.gms.locationsharingreporter.service.START"),
    OCR(279, "com.google.android.gms.ocr.service.START"),
    OCR_INTERNAL(281, "com.google.android.gms.ocr.service.internal.START"),
    IN_APP_REACH(315, "com.google.android.gms.inappreach.service.START")
    ;

    public int SERVICE_ID;
    public String ACTION;
    public String[] SECONDARY_ACTIONS;

    GmsService(int serviceId, String... actions) {
        this.SERVICE_ID = serviceId;
        this.ACTION = actions.length > 0 ? actions[0] : null;
        this.SECONDARY_ACTIONS = actions;
    }

    public interface ADVERTISING_ID {
        // Has no service id
        String ACTION = "com.google.android.gms.ads.identifier.service.START";
    }

    public static GmsService byServiceId(int serviceId) {
        for (GmsService service : values()) {
            if (service.SERVICE_ID == serviceId) return service;
        }
        return UNKNOWN;
    }

    public static GmsService byAction(String action) {
        for (GmsService service : values()) {
            for (String serviceAction : service.SECONDARY_ACTIONS) {
                if (serviceAction.equals(action)) return service;
            }
        }
        return UNKNOWN;
    }

    public static String nameFromServiceId(int serviceId) {
        return byServiceId(serviceId).toString(serviceId);
    }

    public String toString(int serviceId) {
        if (this != UNKNOWN) return toString();
        return "UNKNOWN(" + serviceId + ")";
    }
}
