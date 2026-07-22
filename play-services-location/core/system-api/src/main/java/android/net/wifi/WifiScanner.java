/*
 * SPDX-FileCopyrightText: 2008, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package android.net.wifi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.WorkSource;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * This class provides a way to scan the Wifi universe around the device
 *
 * @hide
 */
public class WifiScanner {

    /**
     * @hide
     */
    public static final int WIFI_BAND_INDEX_24_GHZ = 0;
    /**
     * @hide
     */
    public static final int WIFI_BAND_INDEX_5_GHZ = 1;
    /**
     * @hide
     */
    public static final int WIFI_BAND_INDEX_5_GHZ_DFS_ONLY = 2;
    /**
     * @hide
     */
    public static final int WIFI_BAND_INDEX_6_GHZ = 3;
    /**
     * @hide
     */
    public static final int WIFI_BAND_INDEX_60_GHZ = 4;
    /**
     * @hide
     */
    public static final int WIFI_BAND_COUNT = 5;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            WIFI_BAND_INDEX_24_GHZ,
            WIFI_BAND_INDEX_5_GHZ,
            WIFI_BAND_INDEX_5_GHZ_DFS_ONLY,
            WIFI_BAND_INDEX_6_GHZ,
            WIFI_BAND_INDEX_60_GHZ})
    public @interface WifiBandIndex {
    }

    /**
     * no band specified; use channel list instead
     */
    public static final int WIFI_BAND_UNSPECIFIED = 0;
    /**
     * 2.4 GHz band
     */
    public static final int WIFI_BAND_24_GHZ = 1 << WIFI_BAND_INDEX_24_GHZ;
    /**
     * 5 GHz band excluding DFS channels
     */
    public static final int WIFI_BAND_5_GHZ = 1 << WIFI_BAND_INDEX_5_GHZ;
    /**
     * DFS channels from 5 GHz band only
     */
    public static final int WIFI_BAND_5_GHZ_DFS_ONLY = 1 << WIFI_BAND_INDEX_5_GHZ_DFS_ONLY;
    /**
     * 6 GHz band
     */
    public static final int WIFI_BAND_6_GHZ = 1 << WIFI_BAND_INDEX_6_GHZ;
    /**
     * 60 GHz band
     */
    public static final int WIFI_BAND_60_GHZ = 1 << WIFI_BAND_INDEX_60_GHZ;

    /**
     * Combination of bands
     * Note that those are only the common band combinations,
     * other combinations can be created by combining any of the basic bands above
     */
    /**
     * Both 2.4 GHz band and 5 GHz band; no DFS channels
     */
    public static final int WIFI_BAND_BOTH = WIFI_BAND_24_GHZ | WIFI_BAND_5_GHZ;
    /**
     * 2.4Ghz band + DFS channels from 5 GHz band only
     *
     * @hide
     */
    public static final int WIFI_BAND_24_GHZ_WITH_5GHZ_DFS =
            WIFI_BAND_24_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;
    /**
     * 5 GHz band including DFS channels
     */
    public static final int WIFI_BAND_5_GHZ_WITH_DFS = WIFI_BAND_5_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;
    /**
     * Both 2.4 GHz band and 5 GHz band; with DFS channels
     */
    public static final int WIFI_BAND_BOTH_WITH_DFS =
            WIFI_BAND_24_GHZ | WIFI_BAND_5_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;
    /**
     * 2.4 GHz band and 5 GHz band (no DFS channels) and 6 GHz
     */
    public static final int WIFI_BAND_24_5_6_GHZ = WIFI_BAND_BOTH | WIFI_BAND_6_GHZ;
    /**
     * 2.4 GHz band and 5 GHz band; with DFS channels and 6 GHz
     */
    public static final int WIFI_BAND_24_5_WITH_DFS_6_GHZ =
            WIFI_BAND_BOTH_WITH_DFS | WIFI_BAND_6_GHZ;
    /**
     * @hide
     */
    public static final int WIFI_BAND_24_5_6_60_GHZ =
            WIFI_BAND_24_5_6_GHZ | WIFI_BAND_60_GHZ;
    /**
     * @hide
     */
    public static final int WIFI_BAND_24_5_WITH_DFS_6_60_GHZ =
            WIFI_BAND_24_5_6_60_GHZ | WIFI_BAND_5_GHZ_DFS_ONLY;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            WIFI_BAND_UNSPECIFIED,
            WIFI_BAND_24_GHZ,
            WIFI_BAND_5_GHZ,
            WIFI_BAND_BOTH,
            WIFI_BAND_5_GHZ_DFS_ONLY,
            WIFI_BAND_24_GHZ_WITH_5GHZ_DFS,
            WIFI_BAND_5_GHZ_WITH_DFS,
            WIFI_BAND_BOTH_WITH_DFS,
            WIFI_BAND_6_GHZ,
            WIFI_BAND_24_5_6_GHZ,
            WIFI_BAND_24_5_WITH_DFS_6_GHZ,
            WIFI_BAND_60_GHZ,
            WIFI_BAND_24_5_6_60_GHZ,
            WIFI_BAND_24_5_WITH_DFS_6_60_GHZ})
    public @interface WifiBand {
    }

    /**
     * All bands
     *
     * @hide
     */
    public static final int WIFI_BAND_ALL = (1 << WIFI_BAND_COUNT) - 1;

    /**
     * Minimum supported scanning period
     */
    public static final int MIN_SCAN_PERIOD_MS = 1000;
    /**
     * Maximum supported scanning period
     */
    public static final int MAX_SCAN_PERIOD_MS = 1024000;

    /**
     * No Error
     */
    public static final int REASON_SUCCEEDED = 0;
    /**
     * Unknown error
     */
    public static final int REASON_UNSPECIFIED = -1;
    /**
     * Invalid listener
     */
    public static final int REASON_INVALID_LISTENER = -2;
    /**
     * Invalid request
     */
    public static final int REASON_INVALID_REQUEST = -3;
    /**
     * Invalid request
     */
    public static final int REASON_NOT_AUTHORIZED = -4;
    /**
     * An outstanding request with the same listener hasn't finished yet.
     */
    public static final int REASON_DUPLICATE_REQEUST = -5;

    /**
     * @hide
     */
    public static final String GET_AVAILABLE_CHANNELS_EXTRA = "Channels";

    /**
     * This constant is used for {@link ScanSettings#setRnrSetting(int)}.
     * <p>
     * Scan 6Ghz APs co-located with 2.4/5Ghz APs using Reduced Neighbor Report (RNR) if the 6Ghz
     * band is explicitly requested to be scanned and the current country code supports scanning
     * of at least one 6Ghz channel. The 6Ghz band is explicitly requested if the
     * ScanSetting.band parameter is set to one of:
     * <li> {@link #WIFI_BAND_6_GHZ} </li>
     * <li> {@link #WIFI_BAND_24_5_6_GHZ} </li>
     * <li> {@link #WIFI_BAND_24_5_WITH_DFS_6_GHZ} </li>
     * <li> {@link #WIFI_BAND_24_5_6_60_GHZ} </li>
     * <li> {@link #WIFI_BAND_24_5_WITH_DFS_6_60_GHZ} </li>
     * <li> {@link #WIFI_BAND_ALL} </li>
     **/
    public static final int WIFI_RNR_ENABLED_IF_WIFI_BAND_6_GHZ_SCANNED = 0;
    /**
     * This constant is used for {@link ScanSettings#setRnrSetting(int)}.
     * <p>
     * Request to scan 6Ghz APs co-located with 2.4/5Ghz APs using Reduced Neighbor Report (RNR)
     * when the current country code supports scanning of at least one 6Ghz channel.
     **/
    public static final int WIFI_RNR_ENABLED = 1;
    /**
     * This constant is used for {@link ScanSettings#setRnrSetting(int)}.
     * <p>
     * Do not request to scan 6Ghz APs co-located with 2.4/5Ghz APs using
     * Reduced Neighbor Report (RNR)
     **/
    public static final int WIFI_RNR_NOT_NEEDED = 2;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            WIFI_RNR_ENABLED_IF_WIFI_BAND_6_GHZ_SCANNED,
            WIFI_RNR_ENABLED,
            WIFI_RNR_NOT_NEEDED})
    public @interface RnrSetting {
    }

    /**
     * Generic action callback invocation interface
     *
     * @hide
     */
    public static interface ActionListener {
        public void onSuccess();

        public void onFailure(int reason, String description);
    }

    /**
     * Test if scan is a full scan. i.e. scanning all available bands.
     * For backward compatibility, since some apps don't include 6GHz or 60Ghz in their requests
     * yet, lacking 6GHz or 60Ghz band does not cause the result to be false.
     *
     * @param bandsScanned bands that are fully scanned
     * @param excludeDfs   when true, DFS band is excluded from the check
     * @return true if all bands are scanned, false otherwise
     * @hide
     */
    public static boolean isFullBandScan(@WifiBand int bandsScanned, boolean excludeDfs) {
        return (bandsScanned | WIFI_BAND_6_GHZ | WIFI_BAND_60_GHZ
                | (excludeDfs ? WIFI_BAND_5_GHZ_DFS_ONLY : 0))
                == WIFI_BAND_ALL;
    }

    /**
     * Returns a list of all the possible channels for the given band(s).
     *
     * @param band one of the WifiScanner#WIFI_BAND_* constants, e.g. {@link #WIFI_BAND_24_GHZ}
     * @return a list of all the frequencies, in MHz, for the given band(s) e.g. channel 1 is
     * 2412, or null if an error occurred.
     * @hide
     */
    @NonNull
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public List<Integer> getAvailableChannels(int band) {
        throw new UnsupportedOperationException();
    }

    /**
     * provides channel specification for scanning
     */
    public static class ChannelSpec {
        /**
         * channel frequency in MHz; for example channel 1 is specified as 2412
         */
        public int frequency;
        /**
         * if true, scan this channel in passive fashion.
         * This flag is ignored on DFS channel specification.
         *
         * @hide
         */
        public boolean passive;                                    /* ignored on DFS channels */
        /**
         * how long to dwell on this channel
         *
         * @hide
         */
        public int dwellTimeMS;                                    /* not supported for now */

        /**
         * default constructor for channel spec
         */
        public ChannelSpec(int frequency) {
            this.frequency = frequency;
            passive = false;
            dwellTimeMS = 0;
        }
    }

    /**
     * reports {@link ScanListener#onResults} when underlying buffers are full
     * this is simply the lack of the {@link #REPORT_EVENT_AFTER_EACH_SCAN} flag
     *
     * @deprecated It is not supported anymore.
     */
    @Deprecated
    public static final int REPORT_EVENT_AFTER_BUFFER_FULL = 0;
    /**
     * reports {@link ScanListener#onResults} after each scan
     */
    public static final int REPORT_EVENT_AFTER_EACH_SCAN = (1 << 0);
    /**
     * reports {@link ScanListener#onFullResult} whenever each beacon is discovered
     */
    public static final int REPORT_EVENT_FULL_SCAN_RESULT = (1 << 1);
    /**
     * Do not place scans in the chip's scan history buffer
     */
    public static final int REPORT_EVENT_NO_BATCH = (1 << 2);

    /**
     * Optimize the scan for lower latency.
     *
     * @see ScanSettings#type
     */
    public static final int SCAN_TYPE_LOW_LATENCY = 0;
    /**
     * Optimize the scan for lower power usage.
     *
     * @see ScanSettings#type
     */
    public static final int SCAN_TYPE_LOW_POWER = 1;
    /**
     * Optimize the scan for higher accuracy.
     *
     * @see ScanSettings#type
     */
    public static final int SCAN_TYPE_HIGH_ACCURACY = 2;
    /**
     * Max valid value of SCAN_TYPE_
     *
     * @hide
     */
    public static final int SCAN_TYPE_MAX = 2;

    /**
     * {@hide}
     */
    public static final String SCAN_PARAMS_SCAN_SETTINGS_KEY = "ScanSettings";
    /**
     * {@hide}
     */
    public static final String SCAN_PARAMS_WORK_SOURCE_KEY = "WorkSource";
    /**
     * {@hide}
     */
    public static final String REQUEST_PACKAGE_NAME_KEY = "PackageName";
    /**
     * {@hide}
     */
    public static final String REQUEST_FEATURE_ID_KEY = "FeatureId";

    /**
     * scan configuration parameters to be sent to {@link #startBackgroundScan}
     */
    public static class ScanSettings implements Parcelable {
        /**
         * Hidden network to be scanned for.
         */
        public static class HiddenNetwork {
            /**
             * SSID of the network
             */
            @NonNull
            public final String ssid;

            /**
             * Default constructor for HiddenNetwork.
             */
            public HiddenNetwork(@NonNull String ssid) {
                this.ssid = ssid;
            }
        }

        /**
         * one of the WIFI_BAND values
         */
        public int band;
        /**
         * one of the {@code WIFI_RNR_*} values.
         */
        private int mRnrSetting = WIFI_RNR_ENABLED_IF_WIFI_BAND_6_GHZ_SCANNED;

        /**
         * See {@link #set6GhzPscOnlyEnabled}
         */
        private boolean mEnable6GhzPsc = false;

        /**
         * list of channels; used when band is set to WIFI_BAND_UNSPECIFIED
         */
        public ChannelSpec[] channels;
        /**
         * List of hidden networks to scan for. Explicit probe requests are sent out for such
         * networks during scan. Only valid for single scan requests.
         */
        @NonNull
        public final List<HiddenNetwork> hiddenNetworks = new ArrayList<>();
        /**
         * period of background scan; in millisecond, 0 => single shot scan
         *
         * @deprecated Background scan support has always been hardware vendor dependent. This
         * support may not be present on newer devices. Use {@link #startScan(ScanSettings,
         * ScanListener)} instead for single scans.
         */
        @Deprecated
        public int periodInMs;
        /**
         * must have a valid REPORT_EVENT value
         *
         * @deprecated Background scan support has always been hardware vendor dependent. This
         * support may not be present on newer devices. Use {@link #startScan(ScanSettings,
         * ScanListener)} instead for single scans.
         */
        @Deprecated
        public int reportEvents;
        /**
         * defines number of bssids to cache from each scan
         *
         * @deprecated Background scan support has always been hardware vendor dependent. This
         * support may not be present on newer devices. Use {@link #startScan(ScanSettings,
         * ScanListener)} instead for single scans.
         */
        @Deprecated
        public int numBssidsPerScan;
        /**
         * defines number of scans to cache; use it with REPORT_EVENT_AFTER_BUFFER_FULL
         * to wake up at fixed interval
         *
         * @deprecated Background scan support has always been hardware vendor dependent. This
         * support may not be present on newer devices. Use {@link #startScan(ScanSettings,
         * ScanListener)} instead for single scans.
         */
        @Deprecated
        public int maxScansToCache;
        /**
         * if maxPeriodInMs is non zero or different than period, then this bucket is
         * a truncated binary exponential backoff bucket and the scan period will grow
         * exponentially as per formula: actual_period(N) = period * (2 ^ (N/stepCount))
         * to maxPeriodInMs
         *
         * @deprecated Background scan support has always been hardware vendor dependent. This
         * support may not be present on newer devices. Use {@link #startScan(ScanSettings,
         * ScanListener)} instead for single scans.
         */
        @Deprecated
        public int maxPeriodInMs;
        /**
         * for truncated binary exponential back off bucket, number of scans to perform
         * for a given period
         *
         * @deprecated Background scan support has always been hardware vendor dependent. This
         * support may not be present on newer devices. Use {@link #startScan(ScanSettings,
         * ScanListener)} instead for single scans.
         */
        @Deprecated
        public int stepCount;
        /**
         * Flag to indicate if the scan settings are targeted for PNO scan.
         * {@hide}
         */
        public boolean isPnoScan;
        public int type = SCAN_TYPE_LOW_LATENCY;
        /**
         * This scan request may ignore location settings while receiving scans. This should only
         * be used in emergency situations.
         * {@hide}
         */
        public boolean ignoreLocationSettings;
        /**
         * This scan request will be hidden from app-ops noting for location information. This
         * should only be used by FLP/NLP module on the device which is using the scan results to
         * compute results for behalf on their clients. FLP/NLP module using this flag should ensure
         * that they note in app-ops the eventual delivery of location information computed using
         * these results to their client .
         * {@hide}
         */
        public boolean hideFromAppOps;

        /**
         * Configure whether it is needed to scan 6Ghz non Preferred Scanning Channels when scanning
         * {@link #WIFI_BAND_6_GHZ}. If set to true and a band that contains
         * {@link #WIFI_BAND_6_GHZ} is configured for scanning, then only scan 6Ghz PSC channels in
         * addition to any other bands configured for scanning. Note, 6Ghz non-PSC channels that
         * are co-located with 2.4/5Ghz APs could still be scanned via the
         * {@link #setRnrSetting(int)} API.
         *
         * <p>
         * For example, given a ScanSettings with band set to {@link #WIFI_BAND_24_5_WITH_DFS_6_GHZ}
         * If this API is set to "true" then the ScanSettings is configured to scan all of 2.4Ghz
         * + all of 5Ghz(DFS and non-DFS) + 6Ghz PSC channels. If this API is set to "false", then
         * the ScanSetting is configured to scan all of 2.4Ghz + all of 5Ghz(DFS and non_DFS)
         * + all of 6Ghz channels.
         *
         * @param enable true to only scan 6Ghz PSC channels, false to scan all 6Ghz channels.
         */
        @RequiresApi(Build.VERSION_CODES.S)
        public void set6GhzPscOnlyEnabled(boolean enable) {
            throw new UnsupportedOperationException();
        }

        /**
         * See {@link #set6GhzPscOnlyEnabled}
         */
        @RequiresApi(Build.VERSION_CODES.S)
        public boolean is6GhzPscOnlyEnabled() {
            throw new UnsupportedOperationException();
        }

        /**
         * Configure when to scan 6Ghz APs co-located with 2.4/5Ghz APs using Reduced
         * Neighbor Report (RNR).
         *
         * @param rnrSetting one of the {@code WIFI_RNR_*} values
         */
        @RequiresApi(Build.VERSION_CODES.S)
        public void setRnrSetting(@RnrSetting int rnrSetting) {
            throw new UnsupportedOperationException();
        }

        /**
         * See {@link #setRnrSetting}
         */
        @RequiresApi(Build.VERSION_CODES.S)
        public @RnrSetting int getRnrSetting() {
            throw new UnsupportedOperationException();
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
            throw new UnsupportedOperationException();
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<ScanSettings> CREATOR =
                new Creator<ScanSettings>() {
                    public ScanSettings createFromParcel(Parcel in) {
                        throw new UnsupportedOperationException();
                    }

                    public ScanSettings[] newArray(int size) {
                        return new ScanSettings[size];
                    }
                };
    }

    /**
     * All the information garnered from a single scan
     */
    public static class ScanData implements Parcelable {
        /**
         * scan identifier
         */
        private int mId;
        /**
         * additional information about scan
         * 0 => no special issues encountered in the scan
         * non-zero => scan was truncated, so results may not be complete
         */
        private int mFlags;
        /**
         * Indicates the buckets that were scanned to generate these results.
         * This is not relevant to WifiScanner API users and is used internally.
         * {@hide}
         */
        private int mBucketsScanned;
        /**
         * Bands scanned. One of the WIFI_BAND values.
         * Will be {@link #WIFI_BAND_UNSPECIFIED} if the list of channels do not fully cover
         * any of the bands.
         * {@hide}
         */
        private int mScannedBands;
        /**
         * all scan results discovered in this scan, sorted by timestamp in ascending order
         */
        private final List<ScanResult> mResults;

        ScanData() {
            mResults = new ArrayList<>();
        }

        public ScanData(int id, int flags, ScanResult[] results) {
            mId = id;
            mFlags = flags;
            mResults = new ArrayList<>(Arrays.asList(results));
        }

        /**
         * {@hide}
         */
        public ScanData(int id, int flags, int bucketsScanned, int bandsScanned,
                        ScanResult[] results) {
            this(id, flags, bucketsScanned, bandsScanned, new ArrayList<>(Arrays.asList(results)));
        }

        /**
         * {@hide}
         */
        public ScanData(int id, int flags, int bucketsScanned, int bandsScanned,
                        List<ScanResult> results) {
            mId = id;
            mFlags = flags;
            mBucketsScanned = bucketsScanned;
            mScannedBands = bandsScanned;
            mResults = results;
        }

        public ScanData(ScanData s) {
            throw new UnsupportedOperationException();
        }

        public int getId() {
            return mId;
        }

        public int getFlags() {
            return mFlags;
        }

        /**
         * {@hide}
         */
        public int getBucketsScanned() {
            return mBucketsScanned;
        }

        /**
         * Retrieve the bands that were fully scanned for this ScanData instance. "fully" here
         * refers to all the channels available in the band based on the current regulatory
         * domain.
         *
         * @return Bitmask of {@link #WIFI_BAND_24_GHZ}, {@link #WIFI_BAND_5_GHZ},
         * {@link #WIFI_BAND_5_GHZ_DFS_ONLY}, {@link #WIFI_BAND_6_GHZ} & {@link #WIFI_BAND_60_GHZ}
         * values. Each bit is set only if all the channels in the corresponding band is scanned.
         * Will be {@link #WIFI_BAND_UNSPECIFIED} if the list of channels do not fully cover
         * any of the bands.
         * <p>
         * For ex:
         * <li> Scenario 1:  Fully scanned 2.4Ghz band, partially scanned 5Ghz band
         * - Returns {@link #WIFI_BAND_24_GHZ}
         * </li>
         * <li> Scenario 2:  Partially scanned 2.4Ghz band and 5Ghz band
         * - Returns {@link #WIFI_BAND_UNSPECIFIED}
         * </li>
         * </p>
         */
        public @WifiBand int getScannedBands() {
            return getScannedBandsInternal();
        }

        /**
         * Same as {@link #getScannedBands()}. For use in the wifi stack without version check.
         * <p>
         * {@hide}
         */
        public @WifiBand int getScannedBandsInternal() {
            return mScannedBands;
        }

        public ScanResult[] getResults() {
            return mResults.toArray(new ScanResult[0]);
        }

        /**
         * {@hide}
         */
        public void addResults(@NonNull ScanResult[] newResults) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@hide}
         */
        public void addResults(@NonNull ScanData s) {
            mScannedBands |= s.mScannedBands;
            mFlags |= s.mFlags;
            addResults(s.getResults());
        }

        /**
         * {@hide}
         */
        public boolean isFullBandScanResults() {
            return (mScannedBands & WifiScanner.WIFI_BAND_24_GHZ) != 0
                    && (mScannedBands & WifiScanner.WIFI_BAND_5_GHZ) != 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
            throw new UnsupportedOperationException();
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<ScanData> CREATOR =
                new Creator<ScanData>() {
                    public ScanData createFromParcel(Parcel in) {
                        throw new UnsupportedOperationException();
                    }

                    public ScanData[] newArray(int size) {
                        return new ScanData[size];
                    }
                };
    }

    public static class ParcelableScanData implements Parcelable {

        public ScanData mResults[];

        public ParcelableScanData(ScanData[] results) {
            mResults = results;
        }

        public ScanData[] getResults() {
            return mResults;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
            if (mResults != null) {
                dest.writeInt(mResults.length);
                for (int i = 0; i < mResults.length; i++) {
                    ScanData result = mResults[i];
                    result.writeToParcel(dest, flags);
                }
            } else {
                dest.writeInt(0);
            }
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<ParcelableScanData> CREATOR =
                new Creator<ParcelableScanData>() {
                    public ParcelableScanData createFromParcel(Parcel in) {
                        int n = in.readInt();
                        ScanData results[] = new ScanData[n];
                        for (int i = 0; i < n; i++) {
                            results[i] = ScanData.CREATOR.createFromParcel(in);
                        }
                        return new ParcelableScanData(results);
                    }

                    public ParcelableScanData[] newArray(int size) {
                        return new ParcelableScanData[size];
                    }
                };
    }

    public static class ParcelableScanResults implements Parcelable {

        public ScanResult mResults[];

        public ParcelableScanResults(ScanResult[] results) {
            mResults = results;
        }

        public ScanResult[] getResults() {
            return mResults;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
            if (mResults != null) {
                dest.writeInt(mResults.length);
                for (int i = 0; i < mResults.length; i++) {
                    ScanResult result = mResults[i];
                    result.writeToParcel(dest, flags);
                }
            } else {
                dest.writeInt(0);
            }
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<ParcelableScanResults> CREATOR =
                new Creator<ParcelableScanResults>() {
                    public ParcelableScanResults createFromParcel(Parcel in) {
                        throw new UnsupportedOperationException();
                    }

                    public ParcelableScanResults[] newArray(int size) {
                        return new ParcelableScanResults[size];
                    }
                };
    }

    /**
     * {@hide}
     */
    public static final String PNO_PARAMS_PNO_SETTINGS_KEY = "PnoSettings";
    /**
     * {@hide}
     */
    public static final String PNO_PARAMS_SCAN_SETTINGS_KEY = "ScanSettings";

    /**
     * PNO scan configuration parameters to be sent to {@link #startPnoScan}.
     * Note: This structure needs to be in sync with |wifi_epno_params| struct in gscan HAL API.
     * {@hide}
     */
    public static class PnoSettings implements Parcelable {
        /**
         * Pno network to be added to the PNO scan filtering.
         * {@hide}
         */
        public static class PnoNetwork {
            /*
             * Pno flags bitmask to be set in {@link #PnoNetwork.flags}
             */
            /**
             * Whether directed scan needs to be performed (for hidden SSIDs)
             */
            public static final byte FLAG_DIRECTED_SCAN = (1 << 0);
            /**
             * Whether PNO event shall be triggered if the network is found on A band
             */
            public static final byte FLAG_A_BAND = (1 << 1);
            /**
             * Whether PNO event shall be triggered if the network is found on G band
             */
            public static final byte FLAG_G_BAND = (1 << 2);
            /**
             * Whether strict matching is required
             * If required then the firmware must store the network's SSID and not just a hash
             */
            public static final byte FLAG_STRICT_MATCH = (1 << 3);
            /**
             * If this SSID should be considered the same network as the currently connected
             * one for scoring.
             */
            public static final byte FLAG_SAME_NETWORK = (1 << 4);

            /*
             * Code for matching the beacon AUTH IE - additional codes. Bitmask to be set in
             * {@link #PnoNetwork.authBitField}
             */
            /**
             * Open Network
             */
            public static final byte AUTH_CODE_OPEN = (1 << 0);
            /**
             * WPA_PSK or WPA2PSK
             */
            public static final byte AUTH_CODE_PSK = (1 << 1);
            /**
             * any EAPOL
             */
            public static final byte AUTH_CODE_EAPOL = (1 << 2);

            /**
             * SSID of the network
             */
            public String ssid;
            /**
             * Bitmask of the FLAG_XXX
             */
            public byte flags = 0;
            /**
             * Bitmask of the ATUH_XXX
             */
            public byte authBitField = 0;
            /**
             * frequencies on which the particular network needs to be scanned for
             */
            public int[] frequencies = {};

            /**
             * default constructor for PnoNetwork
             */
            public PnoNetwork(String ssid) {
                this.ssid = ssid;
            }

            @Override
            public int hashCode() {
                return Objects.hash(ssid, flags, authBitField);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof PnoNetwork)) {
                    return false;
                }
                PnoNetwork lhs = (PnoNetwork) obj;
                return TextUtils.equals(this.ssid, lhs.ssid)
                        && this.flags == lhs.flags
                        && this.authBitField == lhs.authBitField;
            }
        }

        /**
         * Connected vs Disconnected PNO flag {@hide}
         */
        public boolean isConnected;
        /**
         * Minimum 5GHz RSSI for a BSSID to be considered
         */
        public int min5GHzRssi;
        /**
         * Minimum 2.4GHz RSSI for a BSSID to be considered
         */
        public int min24GHzRssi;
        /**
         * Minimum 6GHz RSSI for a BSSID to be considered
         */
        public int min6GHzRssi;
        /**
         * Pno Network filter list
         */
        public PnoNetwork[] networkList;

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(isConnected ? 1 : 0);
            dest.writeInt(min5GHzRssi);
            dest.writeInt(min24GHzRssi);
            dest.writeInt(min6GHzRssi);
            if (networkList != null) {
                dest.writeInt(networkList.length);
                for (int i = 0; i < networkList.length; i++) {
                    dest.writeString(networkList[i].ssid);
                    dest.writeByte(networkList[i].flags);
                    dest.writeByte(networkList[i].authBitField);
                    dest.writeIntArray(networkList[i].frequencies);
                }
            } else {
                dest.writeInt(0);
            }
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<PnoSettings> CREATOR =
                new Creator<PnoSettings>() {
                    public PnoSettings createFromParcel(Parcel in) {
                        PnoSettings settings = new PnoSettings();
                        settings.isConnected = in.readInt() == 1;
                        settings.min5GHzRssi = in.readInt();
                        settings.min24GHzRssi = in.readInt();
                        settings.min6GHzRssi = in.readInt();
                        int numNetworks = in.readInt();
                        settings.networkList = new PnoNetwork[numNetworks];
                        for (int i = 0; i < numNetworks; i++) {
                            String ssid = in.readString();
                            PnoNetwork network = new PnoNetwork(ssid);
                            network.flags = in.readByte();
                            network.authBitField = in.readByte();
                            network.frequencies = in.createIntArray();
                            settings.networkList[i] = network;
                        }
                        return settings;
                    }

                    public PnoSettings[] newArray(int size) {
                        return new PnoSettings[size];
                    }
                };

    }

    /**
     * interface to get scan events on; specify this on {@link #startBackgroundScan} or
     * {@link #startScan}
     */
    public interface ScanListener extends ActionListener {
        /**
         * Framework co-ordinates scans across multiple apps; so it may not give exactly the
         * same period requested. If period of a scan is changed; it is reported by this event.
         *
         * @deprecated Background scan support has always been hardware vendor dependent. This
         * support may not be present on newer devices. Use {@link #startScan(ScanSettings,
         * ScanListener)} instead for single scans.
         */
        @Deprecated
        public void onPeriodChanged(int periodInMs);

        /**
         * reports results retrieved from background scan and single shot scans
         */
        public void onResults(ScanData[] results);

        /**
         * reports full scan result for each access point found in scan
         */
        public void onFullResult(ScanResult fullScanResult);
    }

    /**
     * interface to get PNO scan events on; specify this on {@link #startDisconnectedPnoScan} and
     * {@link #startConnectedPnoScan}.
     * {@hide}
     */
    public interface PnoScanListener extends ScanListener {
        /**
         * Invoked when one of the PNO networks are found in scan results.
         */
        void onPnoNetworkFound(ScanResult[] results);
    }

    /**
     * Enable/Disable wifi scanning.
     *
     * @param enable set to true to enable scanning, set to false to disable all types of scanning.
     * @see WifiManager#ACTION_WIFI_SCAN_AVAILABILITY_CHANGED
     * {@hide}
     */
    public void setScanningEnabled(boolean enable) {
        throw new UnsupportedOperationException();
    }

    /**
     * Register a listener that will receive results from all single scans.
     * Either the {@link ScanListener#onSuccess()} or  {@link ScanListener#onFailure(int, String)}
     * method will be called once when the listener is registered.
     * Afterwards (assuming onSuccess was called), all subsequent single scan results will be
     * delivered to the listener. It is possible that onFullResult will not be called for all
     * results of the first scan if the listener was registered during the scan.
     *
     * @param executor the Executor on which to run the callback.
     * @param listener specifies the object to report events to. This object is also treated as a
     *                 key for this request, and must also be specified to cancel the request.
     *                 Multiple requests should also not share this object.
     * @throws SecurityException if the caller does not have permission.
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.LOCATION_HARDWARE})
    public void registerScanListener(@NonNull Executor executor,
                                     @NonNull ScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Overload of {@link #registerScanListener(Executor, ScanListener)} that executes the callback
     * synchronously.
     *
     * @hide
     */
    public void registerScanListener(@NonNull ScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Deregister a listener for ongoing single scans
     *
     * @param listener specifies which scan to cancel; must be same object as passed in {@link
     *                 #registerScanListener}
     */
    public void unregisterScanListener(@NonNull ScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Check whether the Wi-Fi subsystem has started a scan and is waiting for scan results.
     *
     * @return true if a scan initiated via
     * {@link WifiScanner#startScan(ScanSettings, ScanListener)} or
     * {@link WifiManager#startScan()} is in progress.
     * false if there is currently no scanning initiated by {@link WifiScanner} or
     * {@link WifiManager}, but it's still possible the wifi radio is scanning for
     * another reason.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public boolean isScanning() {
        throw new UnsupportedOperationException();
    }

    /**
     * start wifi scan in background
     *
     * @param settings specifies various parameters for the scan; for more information look at
     *                 {@link ScanSettings}
     * @param listener specifies the object to report events to. This object is also treated as a
     *                 key for this scan, and must also be specified to cancel the scan. Multiple
     *                 scans should also not share this object.
     */
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public void startBackgroundScan(ScanSettings settings, ScanListener listener) {
        startBackgroundScan(settings, listener, null);
    }

    /**
     * start wifi scan in background
     *
     * @param settings   specifies various parameters for the scan; for more information look at
     *                   {@link ScanSettings}
     * @param workSource WorkSource to blame for power usage
     * @param listener   specifies the object to report events to. This object is also treated as a
     *                   key for this scan, and must also be specified to cancel the scan. Multiple
     *                   scans should also not share this object.
     * @deprecated Background scan support has always been hardware vendor dependent. This support
     * may not be present on newer devices. Use {@link #startScan(ScanSettings, ScanListener)}
     * instead for single scans.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public void startBackgroundScan(ScanSettings settings, ScanListener listener,
                                    WorkSource workSource) {
        throw new UnsupportedOperationException();
    }

    /**
     * stop an ongoing wifi scan
     *
     * @param listener specifies which scan to cancel; must be same object as passed in {@link
     *                 #startBackgroundScan}
     * @deprecated Background scan support has always been hardware vendor dependent. This support
     * may not be present on newer devices. Use {@link #startScan(ScanSettings, ScanListener)}
     * instead for single scans.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public void stopBackgroundScan(ScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * reports currently available scan results on appropriate listeners
     *
     * @return true if all scan results were reported correctly
     * @deprecated Background scan support has always been hardware vendor dependent. This support
     * may not be present on newer devices. Use {@link #startScan(ScanSettings, ScanListener)}
     * instead for single scans.
     */
    @Deprecated
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public boolean getScanResults() {
        throw new UnsupportedOperationException();
    }

    /**
     * starts a single scan and reports results asynchronously
     *
     * @param settings specifies various parameters for the scan; for more information look at
     *                 {@link ScanSettings}
     * @param listener specifies the object to report events to. This object is also treated as a
     *                 key for this scan, and must also be specified to cancel the scan. Multiple
     *                 scans should also not share this object.
     */
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public void startScan(ScanSettings settings, ScanListener listener) {
        startScan(settings, listener, null);
    }

    /**
     * starts a single scan and reports results asynchronously
     *
     * @param settings   specifies various parameters for the scan; for more information look at
     *                   {@link ScanSettings}
     * @param listener   specifies the object to report events to. This object is also treated as a
     *                   key for this scan, and must also be specified to cancel the scan. Multiple
     *                   scans should also not share this object.
     * @param workSource WorkSource to blame for power usage
     */
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public void startScan(ScanSettings settings, ScanListener listener, WorkSource workSource) {
        startScan(settings, null, listener, workSource);
    }

    /**
     * starts a single scan and reports results asynchronously
     *
     * @param settings   specifies various parameters for the scan; for more information look at
     *                   {@link ScanSettings}
     * @param executor   the Executor on which to run the callback.
     * @param listener   specifies the object to report events to. This object is also treated as a
     *                   key for this scan, and must also be specified to cancel the scan. Multiple
     *                   scans should also not share this object.
     * @param workSource WorkSource to blame for power usage
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public void startScan(ScanSettings settings, @Nullable Executor executor,
                          ScanListener listener, WorkSource workSource) {
        throw new UnsupportedOperationException();
    }

    /**
     * stops an ongoing single shot scan; only useful after {@link #startScan} if onResults()
     * hasn't been called on the listener, ignored otherwise
     *
     * @param listener
     */
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public void stopScan(ScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieve the most recent scan results from a single scan request.
     */
    @NonNull
    @RequiresPermission(android.Manifest.permission.LOCATION_HARDWARE)
    public List<ScanResult> getSingleScanResults() {
        throw new UnsupportedOperationException();
    }

    private void startPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, int key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Start wifi connected PNO scan
     *
     * @param scanSettings specifies various parameters for the scan; for more information look at
     *                     {@link ScanSettings}
     * @param pnoSettings  specifies various parameters for PNO; for more information look at
     *                     {@link PnoSettings}
     * @param executor     the Executor on which to run the callback.
     * @param listener     specifies the object to report events to. This object is also treated as a
     *                     key for this scan, and must also be specified to cancel the scan. Multiple
     *                     scans should also not share this object.
     *                     {@hide}
     */
    public void startConnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings,
                                      @NonNull Executor executor, PnoScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Start wifi disconnected PNO scan
     *
     * @param scanSettings specifies various parameters for the scan; for more information look at
     *                     {@link ScanSettings}
     * @param pnoSettings  specifies various parameters for PNO; for more information look at
     *                     {@link PnoSettings}
     * @param listener     specifies the object to report events to. This object is also treated as a
     *                     key for this scan, and must also be specified to cancel the scan. Multiple
     *                     scans should also not share this object.
     *                     {@hide}
     */
    public void startDisconnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings,
                                         @NonNull Executor executor, PnoScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Stop an ongoing wifi PNO scan
     *
     * @param listener specifies which scan to cancel; must be same object as passed in {@link
     *                 #startPnoScan}
     *                 {@hide}
     */
    public void stopPnoScan(ScanListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * specifies information about an access point of interest
     */
    @Deprecated
    public static class BssidInfo {
        /**
         * bssid of the access point; in XX:XX:XX:XX:XX:XX format
         */
        public String bssid;
        /**
         * low signal strength threshold; more information at {@link ScanResult#level}
         */
        public int low;                                            /* minimum RSSI */
        /**
         * high signal threshold; more information at {@link ScanResult#level}
         */
        public int high;                                           /* maximum RSSI */
        /**
         * channel frequency (in KHz) where you may find this BSSID
         */
        public int frequencyHint;
    }

    /**
     * @hide
     */
    @Deprecated
    public static class WifiChangeSettings implements Parcelable {
        public int rssiSampleSize;                          /* sample size for RSSI averaging */
        public int lostApSampleSize;                        /* samples to confirm AP's loss */
        public int unchangedSampleSize;                     /* samples to confirm no change */
        public int minApsBreachingThreshold;                /* change threshold to trigger event */
        public int periodInMs;                              /* scan period in millisecond */
        public BssidInfo[] bssidInfos;

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<WifiChangeSettings> CREATOR =
                new Creator<WifiChangeSettings>() {
                    public WifiChangeSettings createFromParcel(Parcel in) {
                        return new WifiChangeSettings();
                    }

                    public WifiChangeSettings[] newArray(int size) {
                        return new WifiChangeSettings[size];
                    }
                };

    }

    /**
     * configure WifiChange detection
     *
     * @param rssiSampleSize           number of samples used for RSSI averaging
     * @param lostApSampleSize         number of samples to confirm an access point's loss
     * @param unchangedSampleSize      number of samples to confirm there are no changes
     * @param minApsBreachingThreshold minimum number of access points that need to be
     *                                 out of range to detect WifiChange
     * @param periodInMs               indicates period of scan to find changes
     * @param bssidInfos               access points to watch
     */
    @Deprecated
    @SuppressLint("RequiresPermission")
    public void configureWifiChange(
            int rssiSampleSize,                             /* sample size for RSSI averaging */
            int lostApSampleSize,                           /* samples to confirm AP's loss */
            int unchangedSampleSize,                        /* samples to confirm no change */
            int minApsBreachingThreshold,                   /* change threshold to trigger event */
            int periodInMs,                                 /* period of scan */
            BssidInfo[] bssidInfos                          /* signal thresholds to cross */
    ) {
        throw new UnsupportedOperationException();
    }

    /**
     * interface to get wifi change events on; use this on {@link #startTrackingWifiChange}
     */
    @Deprecated
    public interface WifiChangeListener extends ActionListener {
        /**
         * indicates that changes were detected in wifi environment
         *
         * @param results indicate the access points that exhibited change
         */
        public void onChanging(ScanResult[] results);           /* changes are found */

        /**
         * indicates that no wifi changes are being detected for a while
         *
         * @param results indicate the access points that are bing monitored for change
         */
        public void onQuiescence(ScanResult[] results);         /* changes settled down */
    }

    /**
     * track changes in wifi environment
     *
     * @param listener object to report events on; this object must be unique and must also be
     *                 provided on {@link #stopTrackingWifiChange}
     */
    @Deprecated
    @SuppressLint("RequiresPermission")
    public void startTrackingWifiChange(WifiChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * stop tracking changes in wifi environment
     *
     * @param listener object that was provided to report events on {@link
     *                 #stopTrackingWifiChange}
     */
    @Deprecated
    @SuppressLint("RequiresPermission")
    public void stopTrackingWifiChange(WifiChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * @hide
     */
    @Deprecated
    @SuppressLint("RequiresPermission")
    public void configureWifiChange(WifiChangeSettings settings) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public static interface BssidListener extends ActionListener {
        /**
         * indicates that access points were found by on going scans
         *
         * @param results list of scan results, one for each access point visible currently
         */
        public void onFound(ScanResult[] results);

        /**
         * indicates that access points were missed by on going scans
         *
         * @param results list of scan results, for each access point that is not visible anymore
         */
        public void onLost(ScanResult[] results);
    }

    /**
     * @hide
     */
    @Deprecated
    public static class HotlistSettings implements Parcelable {
        public BssidInfo[] bssidInfos;
        public int apLostThreshold;

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<HotlistSettings> CREATOR =
                new Creator<HotlistSettings>() {
                    public HotlistSettings createFromParcel(Parcel in) {
                        HotlistSettings settings = new HotlistSettings();
                        return settings;
                    }

                    public HotlistSettings[] newArray(int size) {
                        return new HotlistSettings[size];
                    }
                };
    }

    /**
     * set interesting access points to find
     *
     * @param bssidInfos      access points of interest
     * @param apLostThreshold number of scans needed to indicate that AP is lost
     * @param listener        object provided to report events on; this object must be unique and must
     *                        also be provided on {@link #stopTrackingBssids}
     */
    @Deprecated
    @SuppressLint("RequiresPermission")
    public void startTrackingBssids(BssidInfo[] bssidInfos,
                                    int apLostThreshold, BssidListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * remove tracking of interesting access points
     *
     * @param listener same object provided in {@link #startTrackingBssids}
     */
    @Deprecated
    @SuppressLint("RequiresPermission")
    public void stopTrackingBssids(BssidListener listener) {
        throw new UnsupportedOperationException();
    }


    /* private members and methods */

    private static final String TAG = "WifiScanner";
    private static final boolean DBG = false;

    /* commands for Wifi Service */
    private static final int BASE;

    static {
        if (1==1) throw new UnsupportedOperationException();
        BASE = 0;
    }

    /**
     * @hide
     */
    public static final int CMD_START_BACKGROUND_SCAN = BASE + 2;
    /**
     * @hide
     */
    public static final int CMD_STOP_BACKGROUND_SCAN = BASE + 3;
    /**
     * @hide
     */
    public static final int CMD_GET_SCAN_RESULTS = BASE + 4;
    /**
     * @hide
     */
    public static final int CMD_SCAN_RESULT = BASE + 5;
    /**
     * @hide
     */
    public static final int CMD_OP_SUCCEEDED = BASE + 17;
    /**
     * @hide
     */
    public static final int CMD_OP_FAILED = BASE + 18;
    /**
     * @hide
     */
    public static final int CMD_FULL_SCAN_RESULT = BASE + 20;
    /**
     * @hide
     */
    public static final int CMD_START_SINGLE_SCAN = BASE + 21;
    /**
     * @hide
     */
    public static final int CMD_STOP_SINGLE_SCAN = BASE + 22;
    /**
     * @hide
     */
    public static final int CMD_SINGLE_SCAN_COMPLETED = BASE + 23;
    /**
     * @hide
     */
    public static final int CMD_START_PNO_SCAN = BASE + 24;
    /**
     * @hide
     */
    public static final int CMD_STOP_PNO_SCAN = BASE + 25;
    /**
     * @hide
     */
    public static final int CMD_PNO_NETWORK_FOUND = BASE + 26;
    /**
     * @hide
     */
    public static final int CMD_REGISTER_SCAN_LISTENER = BASE + 27;
    /**
     * @hide
     */
    public static final int CMD_DEREGISTER_SCAN_LISTENER = BASE + 28;
    /**
     * @hide
     */
    public static final int CMD_GET_SINGLE_SCAN_RESULTS = BASE + 29;
    /**
     * @hide
     */
    public static final int CMD_ENABLE = BASE + 30;
    /**
     * @hide
     */
    public static final int CMD_DISABLE = BASE + 31;

    private Context mContext;

    private static final int INVALID_KEY = 0;
    private int mListenerKey = 1;

    private final SparseArray mListenerMap = new SparseArray();
    private final SparseArray<Executor> mExecutorMap = new SparseArray<>();
    private final Object mListenerMapLock = new Object();



    // Add a listener into listener map. If the listener already exists, return INVALID_KEY and
    // send an error message to internal handler; Otherwise add the listener to the listener map and
    // return the key of the listener.
    private int addListener(ActionListener listener, Executor executor) {
        throw new UnsupportedOperationException();
    }

    private int putListener(Object listener) {
        if (listener == null) return INVALID_KEY;
        int key;
        synchronized (mListenerMapLock) {
            do {
                key = mListenerKey++;
            } while (key == INVALID_KEY);
            mListenerMap.put(key, listener);
        }
        return key;
    }

    private static class ListenerWithExecutor {
        @Nullable
        final Object mListener;
        @Nullable
        final Executor mExecutor;

        ListenerWithExecutor(@Nullable Object listener, @Nullable Executor executor) {
            mListener = listener;
            mExecutor = executor;
        }
    }

    private ListenerWithExecutor getListenerWithExecutor(int key) {
        if (key == INVALID_KEY) return new ListenerWithExecutor(null, null);
        synchronized (mListenerMapLock) {
            Object listener = mListenerMap.get(key);
            Executor executor = mExecutorMap.get(key);
            return new ListenerWithExecutor(listener, executor);
        }
    }

    private int getListenerKey(Object listener) {
        if (listener == null) return INVALID_KEY;
        synchronized (mListenerMapLock) {
            int index = mListenerMap.indexOfValue(listener);
            if (index == -1) {
                return INVALID_KEY;
            } else {
                return mListenerMap.keyAt(index);
            }
        }
    }

    private Object removeListener(int key) {
        if (key == INVALID_KEY) return null;
        synchronized (mListenerMapLock) {
            Object listener = mListenerMap.get(key);
            mListenerMap.remove(key);
            mExecutorMap.remove(key);
            return listener;
        }
    }

    private int removeListener(Object listener) {
        int key = getListenerKey(listener);
        if (key == INVALID_KEY) {
            Log.e(TAG, "listener cannot be found");
            return key;
        }
        synchronized (mListenerMapLock) {
            mListenerMap.remove(key);
            mExecutorMap.remove(key);
            return key;
        }
    }

    /**
     * @hide
     */
    @VisibleForTesting
    public Handler getInternalHandler() {
        throw new UnsupportedOperationException();
    }

    /**
     * @hide
     */
    public static class OperationResult implements Parcelable {
        public int reason;
        public String description;

        public OperationResult(int reason, String description) {
            this.reason = reason;
            this.description = description;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public int describeContents() {
            return 0;
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(reason);
            dest.writeString(description);
        }

        /**
         * Implement the Parcelable interface {@hide}
         */
        public static final @NonNull Creator<OperationResult> CREATOR =
                new Creator<OperationResult>() {
                    public OperationResult createFromParcel(Parcel in) {
                        int reason = in.readInt();
                        String description = in.readString();
                        return new OperationResult(reason, description);
                    }

                    public OperationResult[] newArray(int size) {
                        return new OperationResult[size];
                    }
                };
    }

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            throw new UnsupportedOperationException();
        }
    }
}