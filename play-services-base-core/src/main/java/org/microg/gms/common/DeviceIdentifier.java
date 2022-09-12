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

import java.util.Random;

public class DeviceIdentifier {
    public String wifiMac = randomMacAddress(); // TODO: static
    public String meid = randomMeid();
    public String esn;


    private static String randomMacAddress() {
        String mac = "b407f9";
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            mac += Integer.toString(rand.nextInt(16), 16);
        }
        return mac;
    }

    private static String randomMeid() {
        // http://en.wikipedia.org/wiki/International_Mobile_Equipment_Identity
        // We start with a known base, and generate random MEID
        String meid = "35503104";
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            meid += Integer.toString(rand.nextInt(10));
        }

        // Luhn algorithm (check digit)
        int sum = 0;
        for (int i = 0; i < meid.length(); i++) {
            int c = Integer.parseInt(String.valueOf(meid.charAt(i)));
            if ((meid.length() - i - 1) % 2 == 0) {
                c *= 2;
                c = c % 10 + c / 10;
            }

            sum += c;
        }
        final int check = (100 - sum) % 10;
        meid += Integer.toString(check);

        return meid;
    }
}
