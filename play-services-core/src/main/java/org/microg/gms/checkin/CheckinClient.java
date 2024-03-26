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

package org.microg.gms.checkin;

import android.content.Context;
import android.util.Log;

import org.microg.gms.common.DeviceConfiguration;
import org.microg.gms.common.DeviceIdentifier;
import org.microg.gms.common.PhoneInfo;
import org.microg.gms.common.Utils;
import org.microg.gms.profile.Build;
import org.microg.gms.profile.ProfileManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CheckinClient {
    private static final String TAG = "GmsCheckinClient";
    private static final Object TODO = null; // TODO
    private static final List<String> TODO_LIST_STRING = new ArrayList<>(); // TODO
    private static final List<CheckinRequest.Checkin.Statistic> TODO_LIST_CHECKIN = new ArrayList<>(); // TODO
    private static final String SERVICE_URL = "https://android.clients.google.com/checkin";

    public static CheckinResponse request(CheckinRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SERVICE_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-type", "application/x-protobuffer");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", "Android-Checkin/2.0 (vbox86p JLS36G); gzip");

        Log.d(TAG, "-- Request --\n" + request);
        OutputStream os = new GZIPOutputStream(connection.getOutputStream());
        os.write(request.encode());
        os.close();

        if (connection.getResponseCode() != 200) {
            try {
                throw new IOException(new String(Utils.readStreamToEnd(new GZIPInputStream(connection.getErrorStream()))));
            } catch (Exception e) {
                throw new IOException(connection.getResponseMessage(), e);
            }
        }

        InputStream is = connection.getInputStream();
        CheckinResponse response = CheckinResponse.ADAPTER.decode(new GZIPInputStream(is));
        is.close();
        return response;
    }

    public static CheckinRequest makeRequest(Context context, DeviceConfiguration deviceConfiguration,
                                             DeviceIdentifier deviceIdent, PhoneInfo phoneInfo,
                                             LastCheckinInfo checkinInfo, Locale locale,
                                             List<Account> accounts, Boolean brandSpoof) {
        ProfileManager.ensureInitialized(context);
        CheckinRequest.Builder builder = new CheckinRequest.Builder()
                .accountCookie(new ArrayList<>())
                .androidId(checkinInfo.getAndroidId())
                .checkin(new CheckinRequest.Checkin.Builder()
                        .build(new CheckinRequest.Checkin.Build.Builder()
                                .bootloader(brandSpoof ? "c2f2-0.2-5799621" : Build.BOOTLOADER)
                                .brand(brandSpoof ? "google" : Build.BRAND)
                                .clientId("android-google")
                                .device(brandSpoof ? "generic" : Build.DEVICE)
                                .fingerprint(brandSpoof ? "google/coral/coral:10/QD1A.190821.007/5831595:user/release-keys" : Build.FINGERPRINT)
                                .hardware(brandSpoof ? "coral" : Build.HARDWARE)
                                .manufacturer(brandSpoof ? "Google" : Build.MANUFACTURER)
                                .model(brandSpoof ? "mainline" : Build.MODEL)
                                .otaInstalled(false) // TODO?
                                //.packageVersionCode(Constants.MAX_REFERENCE_VERSION)
                                .product(brandSpoof ? "coral" : Build.PRODUCT)
                                .radio(brandSpoof ? "" : Build.RADIO)
                                .sdkVersion(brandSpoof ? 29 : Build.VERSION.SDK_INT)
                                .time(Build.TIME / 1000)
                                .build())
                        .cellOperator(phoneInfo.cellOperator)
                        .event(Collections.singletonList(new CheckinRequest.Checkin.Event.Builder()
                                .tag(checkinInfo.getAndroidId() == 0 ? "event_log_start" : "system_update")
                                .value_(checkinInfo.getAndroidId() == 0 ? null : "1536,0,-1,NULL")
                                .timeMs(new Date().getTime())
                                .build()))
                        .lastCheckinMs(checkinInfo.getLastCheckin())
                        .requestedGroup(TODO_LIST_STRING)
                        .roaming(phoneInfo.roaming)
                        .simOperator(phoneInfo.simOperator)
                        .stat(TODO_LIST_CHECKIN)
                        .userNumber(0)
                        .build())
                .deviceConfiguration(new CheckinRequest.DeviceConfig.Builder()
                        .availableFeature(deviceConfiguration.availableFeatures)
                        .densityDpi(deviceConfiguration.densityDpi)
                        .glEsVersion(deviceConfiguration.glEsVersion)
                        .glExtension(deviceConfiguration.glExtensions)
                        .hasFiveWayNavigation(deviceConfiguration.hasFiveWayNavigation)
                        .hasHardKeyboard(deviceConfiguration.hasHardKeyboard)
                        .heightPixels(deviceConfiguration.heightPixels)
                        .keyboardType(deviceConfiguration.keyboardType)
                        .locale(deviceConfiguration.locales)
                        .nativePlatform(deviceConfiguration.nativePlatforms)
                        .navigation(deviceConfiguration.navigation)
                        .screenLayout(deviceConfiguration.screenLayout & 0xF)
                        .sharedLibrary(deviceConfiguration.sharedLibraries)
                        .touchScreen(deviceConfiguration.touchScreen)
                        .widthPixels(deviceConfiguration.widthPixels)
                        .build())
                .digest(checkinInfo.getDigest())
                .esn(deviceIdent.esn)
                .fragment(0)
                .locale(locale.toString())
                .loggingId(new Random().nextLong()) // TODO: static
                .meid(deviceIdent.meid)
                .otaCert(Collections.singletonList("71Q6Rn2DDZl1zPDVaaeEHItd"))
                .serial(Build.SERIAL != null && !Build.SERIAL.isEmpty() ? Build.SERIAL : null)
                .timeZone(TimeZone.getDefault().getID())
                .userName((String) TODO)
                .userSerialNumber((Integer) TODO)
                .version(3);
        for (Account account : accounts) {
            builder.accountCookie.add("[" + account.name + "]");
            builder.accountCookie.add(account.authToken);
        }
        if (builder.accountCookie.isEmpty()) builder.accountCookie.add("");
        if (deviceIdent.wifiMac != null) {
            builder.macAddress(Arrays.asList(deviceIdent.wifiMac))
                    .macAddressType(Arrays.asList("wifi"));
        }
        if (checkinInfo.getSecurityToken() != 0) {
            builder.securityToken(checkinInfo.getSecurityToken())
                    .fragment(1);
        }
        return builder.build();
    }

    public static class Account {
        public final String name;
        public final String authToken;

        public Account(String accountName, String authToken) {
            this.name = accountName;
            this.authToken = authToken;
        }
    }
}
