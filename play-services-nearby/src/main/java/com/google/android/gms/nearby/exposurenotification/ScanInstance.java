/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.exposurenotification;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Information about the sighting of a TEK within a BLE scan (of a few seconds).
 * <p>
 * The TEK itself isn't exposed by the API.
 */
@PublicApi
public class ScanInstance extends AutoSafeParcelable {
    @Field(1)
    private int typicalAttenuationDb;
    @Field(2)
    private int minAttenuationDb;
    @Field(3)
    private int secondsSinceLastScan;

    private ScanInstance() {
    }

    private ScanInstance(int typicalAttenuationDb, int minAttenuationDb, int secondsSinceLastScan) {
        this.typicalAttenuationDb = typicalAttenuationDb;
        this.minAttenuationDb = minAttenuationDb;
        this.secondsSinceLastScan = secondsSinceLastScan;
    }

    /**
     * Minimum attenuation of all of this TEK's beacons received during the scan, in dB.
     */
    public int getMinAttenuationDb() {
        return minAttenuationDb;
    }

    /**
     * Seconds elapsed since the previous scan, typically used as a weight.
     * <p>
     * Two example uses:
     * - Summing those values over all sightings of an exposure provides the duration of that exposure.
     * - Summing those values over all sightings in a given attenuation range and over all exposures recreates the durationAtBuckets of v1.
     * <p>
     * Note that the previous scan may not have led to a sighting of that TEK.
     */
    public int getSecondsSinceLastScan() {
        return secondsSinceLastScan;
    }

    /**
     * Aggregation of the attenuations of all of this TEK's beacons received during the scan, in dB. This is most likely to be an average in the dB domain.
     */
    public int getTypicalAttenuationDb() {
        return typicalAttenuationDb;
    }

    /**
     * Builder for {@link ScanInstance}.
     */
    public static class Builder {
        private int typicalAttenuationDb;
        private int minAttenuationDb;
        private int secondsSinceLastScan;

        public ScanInstance build() {
            return new ScanInstance(typicalAttenuationDb, minAttenuationDb, secondsSinceLastScan);
        }

        public ScanInstance.Builder setMinAttenuationDb(int minAttenuationDb) {
            this.minAttenuationDb = minAttenuationDb;
            return this;
        }

        public ScanInstance.Builder setSecondsSinceLastScan(int secondsSinceLastScan) {
            this.secondsSinceLastScan = secondsSinceLastScan;
            return this;
        }

        public ScanInstance.Builder setTypicalAttenuationDb(int typicalAttenuationDb) {
            this.typicalAttenuationDb = typicalAttenuationDb;
            return this;
        }
    }

    public static final Creator<ScanInstance> CREATOR = new AutoCreator<>(ScanInstance.class);
}
