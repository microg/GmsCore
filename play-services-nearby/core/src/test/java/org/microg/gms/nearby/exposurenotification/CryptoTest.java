/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification;


import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.Arrays;

public class CryptoTest extends TestCase {
    private TemporaryExposureKey key;

    @Override
    protected void setUp() {
        key = new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                .setKeyData(TestVectors.get_TEMPORARY_TRACING_KEY())
                .setRollingStartIntervalNumber(TestVectors.CTINTERVAL_NUMBER_OF_GENERATED_KEY)
                .setRollingPeriod(TestVectors.KEY_ROLLING_PERIOD_MULTIPLE_OF_ID_PERIOD)
                .build();
    }

    @Override
    protected void tearDown() {
        key = null;
    }

    public void testGenerateRpiKey() {
        Assert.assertArrayEquals(CryptoKt.generateRpiKey(key).getEncoded(), TestVectors.get_RPIK());
    }

    public void testGenerateAemKey() {
        Assert.assertArrayEquals(CryptoKt.generateAemKey(key).getEncoded(), TestVectors.get_AEMK());
    }

    public void testGenerateRpiId() {
        for (int i = 0; i < TestVectors.KEY_ROLLING_PERIOD_MULTIPLE_OF_ID_PERIOD; i++) {
            byte[] gen = CryptoKt.generateRpiId(key, key.getRollingStartIntervalNumber() + i);
            Assert.assertArrayEquals(gen, TestVectors.ADVERTISED_DATA.get(i).get_RPI());
        }
    }

    public void testGeneratePayload() {
        for (int i = 0; i < TestVectors.KEY_ROLLING_PERIOD_MULTIPLE_OF_ID_PERIOD; i++) {
            byte[] gen = CryptoKt.generatePayload(key, key.getRollingStartIntervalNumber() + i, TestVectors.get_BLE_METADATA());
            Assert.assertArrayEquals(gen, TestVectors.ADVERTISED_DATA.get(i).get_merged());
        }
    }

    public void testGenerateAllRpiIds() {
        byte[] all = CryptoKt.generateAllRpiIds(key);
        for (int i = 0; i < TestVectors.KEY_ROLLING_PERIOD_MULTIPLE_OF_ID_PERIOD; i++) {
            byte[] ref = CryptoKt.generateRpiId(key, key.getRollingStartIntervalNumber() + i);
            byte[] gen = Arrays.copyOfRange(all, i * 16, (i + 1) * 16);
            Assert.assertArrayEquals(gen, ref);
        }
    }
}
