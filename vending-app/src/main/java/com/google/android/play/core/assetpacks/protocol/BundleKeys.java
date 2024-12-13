/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks.protocol;

import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import org.microg.gms.common.Hide;

import java.util.ArrayList;

@Hide
public final class BundleKeys {
    public static RootKey<Integer> APP_VERSION_CODE = new RootKey.Int("app_version_code");
    public static RootKey<Integer> CHUNK_NUMBER = new RootKey.Int("chunk_number");
    public static RootKey<ParcelFileDescriptor> CHUNK_FILE_DESCRIPTOR = new RootKey.Parcelable<>("chunk_file_descriptor", ParcelFileDescriptor.class);
    public static RootKey<Boolean> KEEP_ALIVE = new RootKey.Bool("keep_alive");
    public static RootKey<String> MODULE_NAME = new RootKey.String("module_name");
    public static RootKey<String> SLICE_ID = new RootKey.String("slice_id");
    public static RootKey<ArrayList<String>> PACK_NAMES = new RootKey.StringArrayList("pack_names");

    // OptionsBundle
    public static RootKey<Integer> PLAY_CORE_VERSION_CODE = new RootKey.Int("playcore_version_code");
    public static RootKey<ArrayList<@CompressionFormat Integer>> SUPPORTED_COMPRESSION_FORMATS = new RootKey.IntArrayList("supported_compression_formats");
    public static RootKey<ArrayList<@PatchFormat Integer>> SUPPORTED_PATCH_FORMATS = new RootKey.IntArrayList("supported_patch_formats");

    // InstalledAssetModulesBundle
    public static RootKey<ArrayList<Bundle>> INSTALLED_ASSET_MODULE = new RootKey.ParcelableArrayList<>("installed_asset_module", Bundle.class);
    public static RootKey<String> INSTALLED_ASSET_MODULE_NAME = new RootKey.String("installed_asset_module_name");
    public static RootKey<Long> INSTALLED_ASSET_MODULE_VERSION = new RootKey.Long("installed_asset_module_version");

    public static RootAndPackKey<Integer> SESSION_ID = new RootAndPackKey.Int("session_id");
    public static RootAndPackKey<Integer> STATUS = new RootAndPackKey.Int("status");
    public static RootAndPackKey<Integer> ERROR_CODE = new RootAndPackKey.Int("error_code");
    public static RootAndPackKey<Long> BYTES_DOWNLOADED = new RootAndPackKey.Long("bytes_downloaded");
    public static RootAndPackKey<Long> TOTAL_BYTES_TO_DOWNLOAD = new RootAndPackKey.Long("total_bytes_to_download");

    public static PackKey<Long> PACK_VERSION = new PackKey.Long("pack_version");
    public static PackKey<Long> PACK_BASE_VERSION = new PackKey.Long("pack_base_version");
    public static PackKey<String> PACK_VERSION_TAG = new PackKey.String("pack_version_tag");
    public static PackKey<ArrayList<String>> SLICE_IDS = new PackKey.StringArrayList("slice_ids");

    public static SliceKey<ArrayList<Intent>> CHUNK_INTENTS = new SliceKey.ParcelableArrayList<>("chunk_intents", Intent.class);
    public static SliceKey<@CompressionFormat Integer> COMPRESSION_FORMAT = new SliceKey.Int("compression_format");
    public static SliceKey<@PatchFormat Integer> PATCH_FORMAT = new SliceKey.Int("patch_format");
    public static SliceKey<String> UNCOMPRESSED_HASH_SHA256 = new SliceKey.String("uncompressed_hash_sha256");
    public static SliceKey<Long> UNCOMPRESSED_SIZE = new SliceKey.Long("uncompressed_size");

    private BundleKeys() {
    }

    @Nullable
    public static <T> T get(Bundle bundle, @NonNull RootKey<T> key) {
        return key.get(bundle, key.baseKey());
    }

    public static <T> T get(Bundle bundle, @NonNull RootKey<T> key, T def) {
        return key.get(bundle, key.baseKey(), def);
    }

    public static <T> void put(Bundle bundle, @NonNull RootKey<T> key, T value) {
        key.put(bundle, key.baseKey(), value);
    }

    @Nullable
    public static <T> T get(Bundle bundle, @NonNull PackKey<T> key, String packName) {
        return key.get(bundle, packKey(packName, key.baseKey()));
    }

    public static <T> T get(Bundle bundle, @NonNull PackKey<T> key, String packName, T def) {
        return key.get(bundle, packKey(packName, key.baseKey()), def);
    }

    public static <T> void put(Bundle bundle, @NonNull PackKey<T> key, String packName, T value) {
        key.put(bundle, packKey(packName, key.baseKey()), value);
    }

    @Nullable
    public static <T> T get(Bundle bundle, @NonNull SliceKey<T> key, String packName, String sliceId) {
        return key.get(bundle, sliceKey(packName, sliceId, key.baseKey()));
    }

    public static <T> T get(Bundle bundle, @NonNull SliceKey<T> key, String packName, String sliceId, T def) {
        return key.get(bundle, sliceKey(packName, sliceId, key.baseKey()), def);
    }

    public static <T> void put(Bundle bundle, @NonNull SliceKey<T> key, String packName, String sliceId, T value) {
        key.put(bundle, sliceKey(packName, sliceId, key.baseKey()), value);
    }

    @NonNull
    private static String packKey(String packName, String baseKey) {
        return baseKey + ":" + packName;
    }

    @NonNull
    private static String sliceKey(String packName, String sliceId, String baseKey) {
        return baseKey + ":" + packName + ":" + sliceId;
    }

    public interface TypedBundleKey<T> {
        @NonNull
        java.lang.String baseKey();

        @Nullable
        T get(@NonNull Bundle bundle, @NonNull java.lang.String key);

        T get(@NonNull Bundle bundle, @NonNull java.lang.String key, T def);

        void put(@NonNull Bundle bundle, @NonNull java.lang.String key, T value);

        abstract class Base<T> implements TypedBundleKey<T> {
            @NonNull
            public final java.lang.String baseKey;

            public Base(@NonNull java.lang.String baseKey) {
                this.baseKey = baseKey;
            }

            @NonNull
            @Override
            public java.lang.String baseKey() {
                return baseKey;
            }
        }

        class Int extends Base<Integer> {

            public Int(@NonNull java.lang.String key) {
                super(key);
            }

            @Override
            public Integer get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return bundle.getInt(key);
            }

            @Override
            public Integer get(@NonNull Bundle bundle, @NonNull java.lang.String key, Integer def) {
                return bundle.getInt(key, def);
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, Integer value) {
                bundle.putInt(key, value);
            }
        }

        class Long extends Base<java.lang.Long> {

            public Long(@NonNull java.lang.String key) {
                super(key);
            }

            @Override
            public java.lang.Long get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return bundle.getLong(key);
            }

            @Override
            public java.lang.Long get(@NonNull Bundle bundle, @NonNull java.lang.String key, java.lang.Long def) {
                return bundle.getLong(key, def);
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, java.lang.Long value) {
                bundle.putLong(key, value);
            }
        }

        class Bool extends Base<Boolean> {

            public Bool(@NonNull java.lang.String key) {
                super(key);
            }

            @Override
            public Boolean get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return bundle.getBoolean(key);
            }

            @Override
            public Boolean get(@NonNull Bundle bundle, @NonNull java.lang.String key, Boolean def) {
                return bundle.getBoolean(key, def);
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, Boolean value) {
                bundle.putBoolean(key, value);
            }
        }

        class String extends Base<java.lang.String> {

            public String(@NonNull java.lang.String key) {
                super(key);
            }

            @Override
            public java.lang.String get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return bundle.getString(key);
            }

            @Override
            public java.lang.String get(@NonNull Bundle bundle, @NonNull java.lang.String key, java.lang.String def) {
                return bundle.getString(key, def);
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, java.lang.String value) {
                bundle.putString(key, value);
            }
        }

        class Parcelable<T extends android.os.Parcelable> extends Base<T> {
            @NonNull
            private final Class<T> tClass;

            public Parcelable(@NonNull java.lang.String key, @NonNull Class<T> tClass) {
                super(key);
                this.tClass = tClass;
            }

            @Override
            public T get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return BundleCompat.getParcelable(bundle, key, tClass);
            }

            @Override
            public T get(@NonNull Bundle bundle, @NonNull java.lang.String key, T def) {
                if (bundle.containsKey(key)) {
                    return BundleCompat.getParcelable(bundle, key, tClass);
                } else {
                    return def;
                }
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, T value) {
                bundle.putParcelable(key, value);
            }
        }

        class StringArrayList extends Base<ArrayList<java.lang.String>> {
            public StringArrayList(@NonNull java.lang.String key) {
                super(key);
            }

            @Override
            public ArrayList<java.lang.String> get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return bundle.getStringArrayList(key);
            }

            @Override
            public ArrayList<java.lang.String> get(@NonNull Bundle bundle, @NonNull java.lang.String key, ArrayList<java.lang.String> def) {
                if (bundle.containsKey(key)) {
                    return bundle.getStringArrayList(key);
                } else {
                    return def;
                }
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, ArrayList<java.lang.String> value) {
                bundle.putStringArrayList(key, value);
            }
        }

        class IntArrayList extends Base<ArrayList<Integer>> {
            public IntArrayList(@NonNull java.lang.String key) {
                super(key);
            }

            @Override
            public ArrayList<Integer> get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return bundle.getIntegerArrayList(key);
            }

            @Override
            public ArrayList<Integer> get(@NonNull Bundle bundle, @NonNull java.lang.String key, ArrayList<Integer> def) {
                if (bundle.containsKey(key)) {
                    return bundle.getIntegerArrayList(key);
                } else {
                    return def;
                }
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, ArrayList<Integer> value) {
                bundle.putIntegerArrayList(key, value);
            }
        }

        class ParcelableArrayList<T extends android.os.Parcelable> extends Base<ArrayList<T>> {
            @NonNull
            private final Class<T> tClass;

            public ParcelableArrayList(@NonNull java.lang.String key, @NonNull Class<T> tClass) {
                super(key);
                this.tClass = tClass;
            }

            @Override
            public ArrayList<T> get(@NonNull Bundle bundle, @NonNull java.lang.String key) {
                return BundleCompat.getParcelableArrayList(bundle, key, tClass);
            }

            @Override
            public ArrayList<T> get(@NonNull Bundle bundle, @NonNull java.lang.String key, ArrayList<T> def) {
                if (bundle.containsKey(key)) {
                    return BundleCompat.getParcelableArrayList(bundle, key, tClass);
                } else {
                    return def;
                }
            }

            @Override
            public void put(@NonNull Bundle bundle, @NonNull java.lang.String key, ArrayList<T> value) {
                bundle.putParcelableArrayList(key, value);
            }
        }
    }


    public interface PackKey<T> extends TypedBundleKey<T> {
        class Int extends TypedBundleKey.Int implements PackKey<Integer> {
            public Int(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class Long extends TypedBundleKey.Long implements PackKey<java.lang.Long> {
            public Long(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class String extends TypedBundleKey.String implements PackKey<java.lang.String> {
            public String(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class StringArrayList extends TypedBundleKey.StringArrayList implements PackKey<ArrayList<java.lang.String>> {
            public StringArrayList(@NonNull java.lang.String key) {
                super(key);
            }
        }
    }

    public interface SliceKey<T> extends TypedBundleKey<T> {
        class Int extends TypedBundleKey.Int implements SliceKey<Integer> {
            public Int(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class Long extends TypedBundleKey.Long implements SliceKey<java.lang.Long> {
            public Long(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class String extends TypedBundleKey.String implements SliceKey<java.lang.String> {
            public String(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class ParcelableArrayList<T extends android.os.Parcelable> extends TypedBundleKey.ParcelableArrayList<T> implements SliceKey<ArrayList<T>> {
            public ParcelableArrayList(@NonNull java.lang.String key, @NonNull Class<T> tClass) {
                super(key, tClass);
            }
        }
    }

    public interface RootKey<T> extends TypedBundleKey<T> {
        class Int extends TypedBundleKey.Int implements RootKey<Integer> {
            public Int(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class Long extends TypedBundleKey.Long implements RootKey<java.lang.Long> {
            public Long(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class Bool extends TypedBundleKey.Bool implements RootKey<Boolean> {
            public Bool(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class String extends TypedBundleKey.String implements RootKey<java.lang.String> {
            public String(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class Parcelable<T extends android.os.Parcelable> extends TypedBundleKey.Parcelable<T> implements RootKey<T> {
            public Parcelable(@NonNull java.lang.String key, @NonNull Class<T> tClass) {
                super(key, tClass);
            }
        }

        class StringArrayList extends TypedBundleKey.StringArrayList implements RootKey<ArrayList<java.lang.String>> {
            public StringArrayList(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class IntArrayList extends TypedBundleKey.IntArrayList implements RootKey<ArrayList<Integer>> {
            public IntArrayList(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class ParcelableArrayList<T extends android.os.Parcelable> extends TypedBundleKey.ParcelableArrayList<T> implements RootKey<ArrayList<T>> {
            public ParcelableArrayList(@NonNull java.lang.String key, @NonNull Class<T> tClass) {
                super(key, tClass);
            }
        }
    }

    public interface RootAndPackKey<T> extends RootKey<T>, PackKey<T> {

        class Int extends TypedBundleKey.Int implements RootAndPackKey<Integer> {
            public Int(@NonNull java.lang.String key) {
                super(key);
            }
        }

        class Long extends TypedBundleKey.Long implements RootAndPackKey<java.lang.Long> {
            public Long(@NonNull java.lang.String key) {
                super(key);
            }
        }
    }

}
