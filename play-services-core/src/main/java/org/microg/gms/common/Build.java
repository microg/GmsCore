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

public class Build {
    public String fingerprint = android.os.Build.FINGERPRINT;
    public String hardware = android.os.Build.HARDWARE;
    public String brand = android.os.Build.BRAND;
    public String radio = getRadio();
    public String bootloader = android.os.Build.BOOTLOADER;
    public long time = android.os.Build.TIME;
    public String device = android.os.Build.DEVICE;
    public int sdk = android.os.Build.VERSION.SDK_INT;
    public String model = android.os.Build.MODEL;
    public String manufacturer = android.os.Build.MANUFACTURER;
    public String product = android.os.Build.PRODUCT;
    public String id = android.os.Build.ID;

    private static String getRadio() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return android.os.Build.getRadioVersion();
        } else {
            //noinspection deprecation
            return android.os.Build.RADIO;
        }
    }
}
