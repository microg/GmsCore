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

package com.google.android.gms.wearable;

import android.os.Bundle;

import org.microg.gms.common.PublicApi;
import org.microg.gms.wearable.databundle.DataBundleUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.microg.gms.wearable.databundle.DataBundleUtil.ASSET_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.BOOLEAN_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.BYTE_ARRAY_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.BYTE_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.DATAMAP_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.DOUBLE_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.FLOAT_ARRAY_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.FLOAT_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.INTEGER_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.LIST_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.LONG_ARRAY_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.LONG_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.STRING_ARRAY_TYPE_CODE;
import static org.microg.gms.wearable.databundle.DataBundleUtil.STRING_TYPE_CODE;

/**
 * A map of data supported by {@link PutDataMapRequest} and {@link DataMapItem}s. DataMap may
 * convert to and from Bundles, but will drop any types not explicitly supported by DataMap in the
 * conversion process.
 */
@PublicApi
public class DataMap {
    public static String TAG = "GmsDataMap";

    private Map<String, Object> data = new HashMap<String, Object>();
    private Map<String, StoredType> types = new HashMap<String, StoredType>();

    public DataMap() {

    }

    /**
     * @return an ArrayList of DataMaps from an ArrayList of Bundles. Any elements in the Bundles not supported by DataMap will be dropped.
     */
    public static ArrayList<DataMap> arrayListFromBundleArrayList(ArrayList<Bundle> bundleArrayList) {
        ArrayList<DataMap> res = new ArrayList<DataMap>();
        for (Bundle bundle : bundleArrayList) {
            res.add(fromBundle(bundle));
        }
        return res;
    }

    /**
     * Removes all elements from the mapping of this DataMap.
     */
    public void clear() {
        data.clear();
    }

    /**
     * @return true if the given key is contained in the mapping of this DataMap.
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    /**
     * @return true if the given Object is a DataMap equivalent to this one.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof DataMap && data.equals(((DataMap) o).data);
    }

    public StoredType getType(String key) {
        return types.get(key);
    }

    @PublicApi(exclude = true)
    public enum StoredType {
        Asset(ASSET_TYPE_CODE), Boolean(BOOLEAN_TYPE_CODE), Byte(BYTE_TYPE_CODE),
        ByteArray(BYTE_ARRAY_TYPE_CODE), DataMap(DATAMAP_TYPE_CODE), DataMapArrayList(DataMap),
        Double(DOUBLE_TYPE_CODE), Float(FLOAT_TYPE_CODE), FloatArray(FLOAT_ARRAY_TYPE_CODE),
        Integer(INTEGER_TYPE_CODE), IntegerArrayList(Integer), Long(LONG_TYPE_CODE),
        LongArray(LONG_ARRAY_TYPE_CODE), String(STRING_TYPE_CODE),
        StringArray(STRING_ARRAY_TYPE_CODE), StringArrayList(String);

        private int typeCode;
        private StoredType listType;

        StoredType(int typeCode) {
            this.typeCode = typeCode;
        }

        StoredType(StoredType listType) {
            this.typeCode = LIST_TYPE_CODE;
            this.listType = listType;
        }

        public int getTypeCode() {
            return typeCode;
        }

        public StoredType getListType() {
            return listType;
        }
    }

    /**
     * @return a DataMap from a Bundle. The input Bundle is expected to contain only elements
     * supported by DataMap. Any elements in the Bundle not supported by DataMap will be dropped.
     */
    public static DataMap fromBundle(Bundle bundle) {
        DataMap res = new DataMap();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object val = bundle.get(key);
                if (val instanceof Asset) {
                    res.putAsset(key, (Asset) val);
                } else if (val instanceof Boolean) {
                    res.putBoolean(key, (Boolean) val);
                } else if (val instanceof Byte) {
                    res.putByte(key, (Byte) val);
                } else if (val instanceof byte[]) {
                    res.putByteArray(key, (byte[]) val);
                } else if (val instanceof Bundle) {
                    res.putDataMap(key, DataMap.fromBundle((Bundle) val));
                } else if (val instanceof Double) {
                    res.putDouble(key, (Double) val);
                } else if (val instanceof Float) {
                    res.putFloat(key, (Float) val);
                } else if (val instanceof float[]) {
                    res.putFloatArray(key, (float[]) val);
                } else if (val instanceof Integer) {
                    res.putInt(key, (Integer) val);
                } else if (val instanceof Long) {
                    res.putLong(key, (Long) val);
                } else if (val instanceof long[]) {
                    res.putLongArray(key, (long[]) val);
                } else if (val instanceof String) {
                    res.putString(key, (String) val);
                } else if (val instanceof String[]) {
                    res.putStringArray(key, (String[]) val);
                } else if (val instanceof ArrayList) {
                    if (((ArrayList) val).isEmpty() || ((ArrayList) val).get(0) instanceof String) {
                        res.putStringArrayList(key, (ArrayList<String>) val);
                    } else if (((ArrayList) val).get(0) instanceof Bundle) {
                        ArrayList<DataMap> dataMaps = new ArrayList<DataMap>();
                        for (Bundle b : ((ArrayList<Bundle>) val)) {
                            dataMaps.add(DataMap.fromBundle(b));
                        }
                        res.putDataMapArrayList(key, dataMaps);
                    } else if (((ArrayList) val).get(0) instanceof Integer) {
                        res.putIntegerArrayList(key, (ArrayList<Integer>) val);
                    }
                }
            }
        }
        return res;
    }

    /**
     * @return a DataMap from a byte[].
     */
    public static DataMap fromByteArray(byte[] bytes) {
        return DataBundleUtil.readDataMap(bytes, Collections.<Asset>emptyList());
    }

    /**
     * @return the entry with the given key as an object, or null
     */
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public Asset getAsset(String key) {
        return types.get(key) == StoredType.Asset ? (Asset) data.get(key) : null;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return types.get(key) == StoredType.Boolean ? (Boolean) data.get(key) : defaultValue;
    }

    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    public byte getByte(String key, byte defaultValue) {
        return types.get(key) == StoredType.Byte ? (Byte) data.get(key) : defaultValue;
    }

    public byte[] getByteArray(String key) {
        return types.get(key) == StoredType.ByteArray ? (byte[]) data.get(key) : null;
    }

    public DataMap getDataMap(String key) {
        return types.get(key) == StoredType.DataMap ? (DataMap) data.get(key) : null;
    }

    public ArrayList<DataMap> getDataMapArrayList(String key) {
        return types.get(key) == StoredType.DataMapArrayList ? (ArrayList<DataMap>) data.get(key) : null;
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        return types.get(key) == StoredType.Double ? (Double) data.get(key) : defaultValue;
    }

    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    public float getFloat(String key, float defaultValue) {
        return types.get(key) == StoredType.Float ? (Float) data.get(key) : defaultValue;
    }

    public float[] getFloatArray(String key) {
        return types.get(key) == StoredType.FloatArray ? (float[]) data.get(key) : null;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        return types.get(key) == StoredType.Integer ? (Integer) data.get(key) : defaultValue;
    }

    public ArrayList<Integer> getIntegerArrayList(String key) {
        return types.get(key) == StoredType.IntegerArrayList ? (ArrayList<Integer>) data.get(key) : null;
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        return types.get(key) == StoredType.Long ? (Long) data.get(key) : defaultValue;
    }

    public long[] getLongArray(String key) {
        return types.get(key) == StoredType.LongArray ? (long[]) data.get(key) : null;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        return types.get(key) == StoredType.String ? (String) data.get(key) : defaultValue;
    }

    public String[] getStringArray(String key) {
        return types.get(key) == StoredType.StringArray ? (String[]) data.get(key) : null;
    }

    public ArrayList<String> getStringArrayList(String key) {
        return types.get(key) == StoredType.StringArrayList ? (ArrayList<String>) data.get(key) : null;
    }

    public int hashCode() {
        return data.hashCode();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Set<String> keySet() {
        return data.keySet();
    }

    public void putAll(DataMap dataMap) {
        for (String key : dataMap.keySet()) {
            data.put(key, dataMap.data.get(key));
            types.put(key, dataMap.types.get(key));
        }
    }

    public void putAsset(String key, Asset value) {
        data.put(key, value);
        types.put(key, StoredType.Asset);
    }

    public void putBoolean(String key, boolean value) {
        data.put(key, value);
        types.put(key, StoredType.Boolean);
    }

    public void putByte(String key, byte value) {
        data.put(key, value);
        types.put(key, StoredType.Byte);
    }

    public void putByteArray(String key, byte[] value) {
        data.put(key, value);
        types.put(key, StoredType.ByteArray);
    }

    public void putDataMap(String key, DataMap value) {
        data.put(key, value);
        types.put(key, StoredType.DataMap);
    }

    public void putDataMapArrayList(String key, ArrayList<DataMap> value) {
        data.put(key, value);
        types.put(key, StoredType.DataMapArrayList);
    }

    public void putDouble(String key, double value) {
        data.put(key, value);
        types.put(key, StoredType.Double);
    }

    public void putFloat(String key, float value) {
        data.put(key, value);
        types.put(key, StoredType.Float);
    }

    public void putFloatArray(String key, float[] value) {
        data.put(key, value);
        types.put(key, StoredType.FloatArray);
    }

    public void putInt(String key, int value) {
        data.put(key, value);
        types.put(key, StoredType.Integer);
    }

    public void putIntegerArrayList(String key, ArrayList<Integer> value) {
        data.put(key, value);
        types.put(key, StoredType.IntegerArrayList);
    }

    public void putLong(String key, long value) {
        data.put(key, value);
        types.put(key, StoredType.Long);
    }

    public void putLongArray(String key, long[] value) {
        data.put(key, value);
        types.put(key, StoredType.LongArray);
    }

    public void putString(String key, String value) {
        data.put(key, value);
        types.put(key, StoredType.String);
    }

    public void putStringArray(String key, String[] value) {
        data.put(key, value);
        types.put(key, StoredType.StringArray);
    }

    public void putStringArrayList(String key, ArrayList<String> value) {
        data.put(key, value);
        types.put(key, StoredType.StringArrayList);
    }

    public Object remove(String key) {
        types.remove(key);
        return data.remove(key);
    }

    public int size() {
        return data.size();
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        for (String key : data.keySet()) {
            switch (types.get(key)) {
                case Asset:
                    bundle.putParcelable(key, (Asset) data.get(key));
                    break;
                case Boolean:
                    bundle.putBoolean(key, (Boolean) data.get(key));
                    break;
                case Byte:
                    bundle.putByte(key, (Byte) data.get(key));
                    break;
                case ByteArray:
                    bundle.putByteArray(key, (byte[]) data.get(key));
                    break;
                case DataMap:
                    bundle.putBundle(key, ((DataMap) data.get(key)).toBundle());
                    break;
                case DataMapArrayList:
                    ArrayList<Bundle> bundles = new ArrayList<Bundle>();
                    for (DataMap dataMap : ((ArrayList<DataMap>) data.get(key))) {
                        bundles.add(dataMap.toBundle());
                    }
                    bundle.putParcelableArrayList(key, bundles);
                    break;
                case Double:
                    bundle.putDouble(key, (Double) data.get(key));
                    break;
                case Float:
                    bundle.putFloat(key, (Float) data.get(key));
                    break;
                case FloatArray:
                    bundle.putFloatArray(key, (float[]) data.get(key));
                    break;
                case Integer:
                    bundle.putInt(key, (Integer) data.get(key));
                    break;
                case IntegerArrayList:
                    bundle.putIntegerArrayList(key, (ArrayList<Integer>) data.get(key));
                    break;
                case Long:
                    bundle.putLong(key, (Long) data.get(key));
                    break;
                case LongArray:
                    bundle.putLongArray(key, (long[]) data.get(key));
                    break;
                case String:
                    bundle.putString(key, (String) data.get(key));
                    break;
                case StringArray:
                    bundle.putStringArray(key, (String[]) data.get(key));
                    break;
                case StringArrayList:
                    bundle.putStringArrayList(key, (ArrayList<String>) data.get(key));
                    break;
            }
        }
        return bundle;
    }

    public byte[] toByteArray() {
        return DataBundleUtil.createBytes(this);
    }

    public String toString() {
        return "DataMap{size=" + size() + "}";
    }
}
