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


import com.google.android.gms.common.BuildConfig;

public enum GmsService {
    UNKNOWN(-2),
    ANY(-1),
    GAMES(1, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.games.service.START", BuildConfig.BASE_PACKAGE_NAME + ".android.gms.games.internal.connect.service.START"),
    PLUS(2, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.plus.service.START", BuildConfig.BASE_PACKAGE_NAME + ".android.gms.plus.service.internal.START"),
    PANORAMA(3, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.panorama.service.START"),
    WALLET(4, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.wallet.service.BIND"),
    PEOPLE(5, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.people.service.START"),
    LOCATION(6),
    APPSTATE(7, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.appstate.service.START"),
    ADREQUEST(8, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ads.service.START"),
    ACCOUNT(9, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.accounts.ACCOUNT_SERVICE"),
    CAST(10, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.cast.service.BIND_CAST_DEVICE_CONTROLLER_SERVICE"),
    DRIVE(11, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.drive.ApiService.START"),
    ADDRESS(12, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.identity.service.BIND"),
    CAR(13, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.car.service.START"),
    WEARABLE(14, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.wearable.BIND"),
    AUTH_PROXY(16, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.service.START"),
    FITNESS(17, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.GoogleFitnessService.START"),
    REMINDERS(18, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.reminders.service.START"),
    LIGHTWEIGHT_INDEX(19, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.icing.LIGHTWEIGHT_INDEX_SERVICE"),
    DEVICE_CONNECTION(20, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.deviceconnection.service.START"),
    INDEX(21, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.icing.INDEX_SERVICE"),
    LOCATION_REPORTING(22, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.location.reporting.service.START", BuildConfig.BASE_PACKAGE_NAME + ".android.location.reporting.service.START"),
    LOCATION_MANAGER(23, BuildConfig.BASE_PACKAGE_NAME + ".android.location.internal.GoogleLocationManagerService.START"),
    PLAY_LOG(24, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.playlog.service.START"),
    DROIDGUARD(25, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.droidguard.service.START"),
    LOCKBOX(26, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.lockbox.service.START"),
    CAST_MIRRORING(27, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.cast_mirroring.service.START"),
    NETWORK_QUALITY(28, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.mdm.services.START"),
    FEEDBACK(29, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.feedback.internal.IFeedbackService"),
    SEARCH_ADMINISTRATION(30),
    PHOTO_AUTO_BACKUP(31, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.photos.autobackup.service.START"),
    SEARCH_QUERIES(32),
    SEARCH_GLOBAL(33),
    UDC(35, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.udc.service.START"),
    SEARCH_CORPORA(36),
    DEVICE_MANAGER(37, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.mdm.services.DeviceManagerApiService.START"),
    PSEUDONYMOUS_ID(38, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.pseudonymous.service.START"),
    COMMON(39, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.common.service.START"),
    CLEARCUT_LOGGER(40, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.clearcut.service.START"),
    USAGE_REPORTING(41, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.usagereporting.service.START"),
    KIDS(42, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.kids.service.START"),
    DOWNLOAD(43, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.common.download.START"),
    SIGN_IN(44, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.signin.service.START"),
    SAFETY_NET_CLIENT(45, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.safetynet.service.START"),
    GSERVICES(46, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ads.gservice.START"),
    CONTEXT_MANAGER(47, BuildConfig.BASE_PACKAGE_NAME + ".android.contextmanager.service.ContextManagerService.START"),
    AUDIO_MODEM(48, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.audiomodem.service.AudioModemService.START"),
    NEARBY_SHARING(49, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.nearby.sharing.service.NearbySharingService.START"),
    LIGHTWEIGHT_NETWORK_QUALITY(50, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.herrevad.services.LightweightNetworkQualityAndroidService.START"),
    PHENOTYPE(51, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.phenotype.service.START"),
    VOICE_UNLOCK(52, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.speech.service.START"),
    NEARBY_CONNECTIONS(54, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.nearby.connection.service.START"),
    FITNESS_SENSORS(55, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.SensorsApi"),
    FITNESS_RECORDING(56, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.RecordingApi"),
    FITNESS_HISTORY(57, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.HistoryApi"),
    FITNESS_SESSIONS(58, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.SessionsApi"),
    FITNESS_BLE(59, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.BleApi"),
    FITNESS_CONFIG(60, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.ConfigApi"),
    FITNESS_INTERNAL(61, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fitness.InternalApi"),
    NEARBY_MESSAGES(62, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.nearby.messages.service.NearbyMessagesService.START"),
    HELP(63, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.googlehelp.service.GoogleHelpService.START"),
    CONFIG(64, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.config.START"),
    GEODATA(65, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.location.places.GeoDataApi"),
    SEARCH_IME(66),
    PLACE_DETECTION(67, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.location.places.PlaceDetectionApi"),
    CREDENTIALS(68, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.api.credentials.service.START"),
    NEARBY_BOOTSTRAP(69, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.nearby.bootstrap.service.NearbyBootstrapService.START"),
    PLUS_INTERNAL(70),
    SOURCE_DEVICE(75, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.smartdevice.d2d.SourceDeviceService.START"),
    TARGET_DEVICE(76, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.smartdevice.d2d.TargetDeviceService.START"),
    APP_INVITE(77, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.appinvite.service.START"),
    TAP_AND_PAY(79, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.tapandpay.service.BIND"),
    CHROME_SYNC(80, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.chromesync.service.START"),
    ACCOUNTS(81, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.smartdevice.setup.accounts.AccountsService.START"),
    CAST_REMOTE_DISPLAY(83, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.cast.remote_display.service.START"),
    TRUST_AGENT(85, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.trustagent.StateApi.START"),
    AUTH_SIGN_IN(91, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.api.signin.service.START"),
    MEASUREMENT(93, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.measurement.START"),
    FREIGHTER(98, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.freighter.service.START"),
    GUNS(110, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.notifications.service.START"),
    BLE(111, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.beacon.internal.IBleService.START"),
    FIREBASE_AUTH(112, BuildConfig.BASE_PACKAGE_NAME + ".firebase.auth.api.gms.service.START"),
    APP_INDEXING(113),
    GASS(116, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.gass.START"),
    WORK_ACCOUNT(120),
    INSTANT_APPS(121, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.instantapps.START"),
    CAST_FIRSTPATY(122, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.cast.firstparty.START"),
    AD_CACHE(123, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ads.service.CACHE"),
    SMS_RETRIEVER(126, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.api.phone.service.SmsRetrieverApiService.START"),
    CRYPT_AUTH(129, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.cryptauth.cryptauthservice.START"),
    DYNAMIC_LINKS(131, BuildConfig.BASE_PACKAGE_NAME + ".firebase.dynamiclinks.service.START"),
    FONTS(132, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fonts.service.START"),
    ROMANESCO(135, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.romanesco.service.START"),
    TRAINER(139, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.learning.trainer.START"),
    FIDO2_REGULAR(148, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fido.fido2.regular.START"),
    FIDO2_PRIVILEGED(149, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fido.fido2.privileged.START"),
    DATA_DOWNLOAD(152, BuildConfig.BASE_PACKAGE_NAME + ".android.mdd.service.START"),
    ACCOUNT_DATA(153, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.account.data.service.START"),
    CONSTELLATION(155, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.constellation.service.START"),
    AUDIT(154, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.audit.service.START"),
    SYSTEM_UPDATE(157, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.update.START_API_SERVICE"),
    MOBSTORE(160, BuildConfig.BASE_PACKAGE_NAME + ".android.mobstore.service.START"),
    USER_LOCATION(163, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.userlocation.service.START"),
    AD_HTTP(166, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ads.service.HTTP"),
    LANGUAGE_PROFILE(167, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.languageprofile.service.START"),
    MDNS(168, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.mdns.service.START"),
    SEMANTIC_LOCATION(173, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.semanticlocation.service.START_ODLH"),
    FIDO2_ZEROPARTY(180, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fido.fido2.zeroparty.START"),
    G1_RESTORE(181, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.backup.G1_RESTORE"),
    G1_BACKUP(182, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.backup.G1_BACKUP"),
    OSS_LICENSES(185, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.oss.licenses.service.START"),
    PAYSE(188, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.payse.service.BIND"),
    RCS(189, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.rcs.START"),
    CARRIER_AUTH(191, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.carrierauth.service.START"),
    SYSTEM_UPDATE_SINGLE_UESR(192, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.update.START_SINGLE_USER_API_SERVICE"),
    APP_USAGE(193, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.appusage.service.START"),
    NEARBY_SHARING_2(194, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.nearby.sharing.START_SERVICE"),
    AD_CONSENT_LOOKUP(195, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ads.service.CONSENT_LOOKUP"),
    CREDENTIAL_MANAGER(196, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.credential.manager.service.firstparty.START"),
    PHONE_INTERNAL(197, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.api.phone.service.InternalService.START"),
    PAY(198, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.pay.service.BIND"),
    ASTERISM(199, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.asterism.service.START"),
    MODULE_RESTORE(201, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.backup.GMS_MODULE_RESTORE"),
    FACS_CACHE(202, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.facs.cache.service.START"),
    RECAPTCHA(205, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.recaptcha.service.START"),
    CONTACT_SYNC(208, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.people.contactssync.service.START"),
    IDENTITY_SIGN_IN(212, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.api.identity.service.signin.START"),
    CREDENTIAL_STORE(214, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fido.credentialstore.internal_service.START"),
    MDI_SYNC(215, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.mdisync.service.START"),
    EVENT_ATTESTATION(216, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ads.identifier.service.EVENT_ATTESTATION"),
    SCHEDULER(218, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.scheduler.ACTION_PROXY_SCHEDULE"),
    AUTHORIZATION(219, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.api.identity.service.authorization.START"),
    FACS_SYNC(220, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.facs.internal.service.START"),
    AUTH_CONFIG_SYNC(221, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.config.service.START"),
    CREDENTIAL_SAVING(223, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.api.identity.service.credentialsaving.START"),
    GOOGLE_AUTH(224, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.account.authapi.START"),
    ENTERPRISE_LOADER(225, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.enterprise.loader.service.START"),
    THUNDERBIRD(226, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.thunderbird.service.START"),
    NEARBY_EXPOSURE(236, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.nearby.exposurenotification.START"),
    GMS_COMPLIANCE(257, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.gmscompliance.service.START"),
    BLOCK_STORE(258, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.auth.blockstore.service.START"),
    FIDO_SOURCE_DEVICE(262, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fido.sourcedevice.service.START"),
    FAST_PAIR(265, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.nearby.fastpair.START"),
    MATCHSTICK_LIGHTER(268, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.matchstick.lighter.service.START"),
    FIDO_TARGET_DEVICE_INTERNAL(269, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.fido.targetdevice.internal_service.START"),
    TELEMETRY(270, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.common.telemetry.service.START"),
    SECOND_DEVICE_AUTH(275, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.setup.auth.SecondDeviceAuth.START"),
    LOCATION_SHARING_REPORTER(277, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.locationsharingreporter.service.START"),
    OCR(279, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ocr.service.START"),
    POTOKENS(285, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.potokens.service.START"),
    OCR_INTERNAL(281, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ocr.service.internal.START"),
    APP_SET(300, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.appset.service.START"),
    MODULE_INSTALL(308, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.chimera.container.moduleinstall.ModuleInstallService.START"),
    IN_APP_REACH(315, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.inappreach.service.START"),
    APP_ERRORS(334, BuildConfig.BASE_PACKAGE_NAME + ".android.gms.apperrors.service.START_APP_ERROR"),
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
        String ACTION = BuildConfig.BASE_PACKAGE_NAME + ".android.gms.ads.identifier.service.START";
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
