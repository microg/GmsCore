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

import android.annotation.TargetApi;

import java.util.Locale;
import java.util.Random;

// TODO: Make flexible
public class Build {
    public String board = android.os.Build.BOARD;
    public String bootloader = android.os.Build.BOOTLOADER;
    public String brand = android.os.Build.BRAND;
    public String cpu_abi = android.os.Build.CPU_ABI;
    public String cpu_abi2 = android.os.Build.CPU_ABI2;
    @TargetApi(21)
    public String[] supported_abis = android.os.Build.VERSION.SDK_INT >= 21 ? android.os.Build.SUPPORTED_ABIS : new String[0];
    public String device = android.os.Build.DEVICE;
    public String display = android.os.Build.DISPLAY;
    public String fingerprint = android.os.Build.FINGERPRINT;
    public String hardware = android.os.Build.HARDWARE;
    public String host = android.os.Build.HOST;
    public String id = android.os.Build.ID;
    public String manufacturer = android.os.Build.MANUFACTURER;
    public String model = android.os.Build.MODEL;
    public String product = android.os.Build.PRODUCT;
    public String radio = android.os.Build.RADIO;
    public String serial = generateSerialNumber(); // TODO: static
    public String tags = android.os.Build.TAGS;
    public long time = android.os.Build.TIME;
    public String type = android.os.Build.TYPE;
    public String user = android.os.Build.USER;
    public String version_codename = android.os.Build.VERSION.CODENAME;
    public String version_incremental = android.os.Build.VERSION.INCREMENTAL;
    public String version_release = android.os.Build.VERSION.RELEASE;
    public String version_sdk = android.os.Build.VERSION.SDK;
    public int version_sdk_int = android.os.Build.VERSION.SDK_INT;

    private String generateSerialNumber() {
        String serial = "008741";
        Random rand = new Random();
        for (int i = 0; i < 10; i++)
            serial += Integer.toString(rand.nextInt(16), 16);
        serial = serial.toUpperCase(Locale.US);
        return serial;
    }
}
