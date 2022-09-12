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

package org.microg.gms.wearable.databundle;

import android.util.SparseArray;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okio.ByteString;

public class DataBundleUtil {

    public static DataMap readDataMap(byte[] bytes, List<Asset> assets) {
        try {
            return readDataMap(DataBundle.ADAPTER.decode(bytes), assets);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataMap readDataMap(DataBundle dataBundle, List<Asset> assets) {
        return readDataMap(dataBundle.entries, assets);
    }

    public static DataMap readDataMap(List<DataBundleEntry> entries, List<Asset> assets) {
        DataMap dataMap = new DataMap();
        for (DataBundleEntry entry : entries) {
            readAndStore(dataMap, entry.key, entry.typedValue, assets);
        }
        return dataMap;
    }

    public static byte[] createBytes(DataMap dataMap) {
        AssetAnnotatedDataBundle dataBundle = createDataBundle(dataMap);
        if (!dataBundle.getAssets().isEmpty()) {
            throw new UnsupportedOperationException();
        }
        return dataBundle.getData();
    }

    public static AssetAnnotatedDataBundle createDataBundle(DataMap dataMap) {
        AssetAnnotatedDataBundle dataBundle = new AssetAnnotatedDataBundle();
        dataBundle.assets = new ArrayList<Asset>();
        dataBundle.dataBundle = new DataBundle.Builder().entries(createEntryList(dataMap, dataBundle.assets)).build();
        return dataBundle;
    }

    private static List<DataBundleEntry> createEntryList(DataMap dataMap, List<Asset> assets) {
        List<DataBundleEntry> entries = new ArrayList<DataBundleEntry>();
        for (String key : dataMap.keySet()) {
            entries.add(getTypeHelper(dataMap.getType(key)).loadAndCreateEntry(dataMap, key, assets));
        }
        return entries;
    }

    private static void readAndStore(DataMap dataMap, String key, DataBundleTypedValue value, List<Asset> assets) {
        if (value.type == null) return;
        getTypeHelper(value.type).readAndStore(dataMap, key, value, assets);
    }


    private static SparseArray<TypeHelper> typeHelperByCode;

    private static void rememberTypeReader(TypeHelper typeHelper) {
        typeHelperByCode.put(typeHelper.type, typeHelper);
    }

    static TypeHelper getTypeHelper(int type) {
        if (typeHelperByCode.get(type) != null) {
            return typeHelperByCode.get(type);
        } else {
            throw new IllegalArgumentException();
        }
    }

    static TypeHelper getTypeHelper(DataMap.StoredType type) {
        return getTypeHelper(type.getTypeCode());
    }

    static TypeHelper getListInnerTypeHelper(DataMap.StoredType type) {
        return getTypeHelper(type.getListType());
    }

    public static final int BYTE_ARRAY_TYPE_CODE = 1;
    static TypeHelper<byte[]> BYTEARRAY = new TypeHelper<byte[]>(BYTE_ARRAY_TYPE_CODE) {
        @Override
        byte[] read(DataBundleValue value, List<Asset> assets) {
            return value.byteArray.toByteArray();
        }

        @Override
        DataBundleValue create(byte[] value, List<Asset> assets) {
            return new DataBundleValue.Builder().byteArray(ByteString.of(value)).build();
        }

        @Override
        void store(DataMap dataMap, String key, byte[] value) {
            dataMap.putByteArray(key, value);
        }

        @Override
        byte[] load(DataMap dataMap, String key) {
            return dataMap.getByteArray(key);
        }
    };

    public static final int STRING_TYPE_CODE = 2;
    static TypeHelper<String> STRING = new TypeHelper<String>(STRING_TYPE_CODE) {
        @Override
        String read(DataBundleValue value, List<Asset> assets) {
            return value.stringVal;
        }

        @Override
        DataBundleValue create(String value, List<Asset> assets) {
            return new DataBundleValue.Builder().stringVal(value).build();
        }

        @Override
        void store(DataMap dataMap, String key, String value) {
            if (value != null) dataMap.putString(key, value);
        }

        @Override
        void storeList(DataMap dataMap, String key, ArrayList<String> valueList) {
            dataMap.putStringArrayList(key, valueList);
        }

        @Override
        String load(DataMap dataMap, String key) {
            return dataMap.getString(key);
        }

        @Override
        AnnotatedArrayList<String> loadList(DataMap dataMap, String key) {
            AnnotatedArrayList<String> list = new AnnotatedArrayList<String>(this);
            list.addAll(dataMap.getStringArrayList(key));
            return list;
        }
    };

    public static final int DOUBLE_TYPE_CODE = 3;
    static TypeHelper<Double> DOUBLE = new TypeHelper<Double>(DOUBLE_TYPE_CODE) {
        @Override
        Double read(DataBundleValue value, List<Asset> assets) {
            return value.doubleVal;
        }

        @Override
        DataBundleValue create(Double value, List<Asset> assets) {
            return new DataBundleValue.Builder().doubleVal(value).build();
        }

        @Override
        void store(DataMap dataMap, String key, Double value) {
            if (value != null) dataMap.putDouble(key, value);
        }

        @Override
        Double load(DataMap dataMap, String key) {
            return dataMap.getDouble(key);
        }
    };

    public static final int FLOAT_TYPE_CODE = 4;
    static TypeHelper<Float> FLOAT = new TypeHelper<Float>(FLOAT_TYPE_CODE) {
        @Override
        Float read(DataBundleValue value, List<Asset> assets) {
            return value.floatVal;
        }

        @Override
        DataBundleValue create(Float value, List<Asset> assets) {
            return new DataBundleValue.Builder().floatVal(value).build();
        }

        @Override
        void store(DataMap dataMap, String key, Float value) {
            if (value != null) dataMap.putFloat(key, value);
        }

        @Override
        Float load(DataMap dataMap, String key) {
            return dataMap.getFloat(key);
        }
    };

    public static final int LONG_TYPE_CODE = 5;
    static TypeHelper<Long> LONG = new TypeHelper<Long>(LONG_TYPE_CODE) {
        @Override
        Long read(DataBundleValue value, List<Asset> assets) {
            return value.longVal;
        }

        @Override
        DataBundleValue create(Long value, List<Asset> assets) {
            return new DataBundleValue.Builder().longVal(value).build();
        }

        @Override
        void store(DataMap dataMap, String key, Long value) {
            if (value != null) dataMap.putLong(key, value);
        }

        @Override
        Long load(DataMap dataMap, String key) {
            return dataMap.getLong(key);
        }
    };

    public static final int INTEGER_TYPE_CODE = 6;
    static TypeHelper<Integer> INTEGER = new TypeHelper<Integer>(INTEGER_TYPE_CODE) {
        @Override
        Integer read(DataBundleValue value, List<Asset> assets) {
            return value.intVal;
        }

        @Override
        DataBundleValue create(Integer value, List<Asset> assets) {
            return new DataBundleValue.Builder().intVal(value).build();
        }

        @Override
        void store(DataMap dataMap, String key, Integer value) {
            if (value != null) dataMap.putInt(key, value);
        }

        @Override
        void storeList(DataMap dataMap, String key, ArrayList<Integer> valueList) {
            dataMap.putIntegerArrayList(key, valueList);
        }

        @Override
        Integer load(DataMap dataMap, String key) {
            return dataMap.getInt(key);
        }

        @Override
        AnnotatedArrayList<Integer> loadList(DataMap dataMap, String key) {
            AnnotatedArrayList<Integer> list = new AnnotatedArrayList<Integer>(this);
            list.addAll(dataMap.getIntegerArrayList(key));
            return list;
        }
    };

    public static final int BYTE_TYPE_CODE = 7;
    static TypeHelper<Byte> BYTE = new TypeHelper<Byte>(BYTE_TYPE_CODE) {
        @Override
        Byte read(DataBundleValue value, List<Asset> assets) {
            return (byte) (int) value.byteVal;
        }

        @Override
        DataBundleValue create(Byte value, List<Asset> assets) {
            return new DataBundleValue.Builder().byteVal((int) (byte) value).build();
        }

        @Override
        void store(DataMap dataMap, String key, Byte value) {
            if (value != null) dataMap.putByte(key, value);
        }

        @Override
        Byte load(DataMap dataMap, String key) {
            return dataMap.getByte(key);
        }
    };

    public static final int BOOLEAN_TYPE_CODE = 8;
    static TypeHelper<Boolean> BOOLEAN = new TypeHelper<Boolean>(BOOLEAN_TYPE_CODE) {
        @Override
        Boolean read(DataBundleValue value, List<Asset> assets) {
            return value.booleanVal;
        }

        @Override
        DataBundleValue create(Boolean value, List<Asset> assets) {
            return new DataBundleValue.Builder().booleanVal(value).build();
        }

        @Override
        void store(DataMap dataMap, String key, Boolean value) {
            if (value != null) dataMap.putBoolean(key, value);
        }

        @Override
        Boolean load(DataMap dataMap, String key) {
            return dataMap.getBoolean(key);
        }
    };

    public static final int DATAMAP_TYPE_CODE = 9;
    static TypeHelper<DataMap> DATAMAP = new TypeHelper<DataMap>(DATAMAP_TYPE_CODE) {
        @Override
        DataMap read(DataBundleValue value, List<Asset> assets) {
            return readDataMap(value.map, assets);
        }

        @Override
        DataBundleValue create(DataMap value, List<Asset> assets) {
            return new DataBundleValue.Builder().map(createEntryList(value, assets)).build();
        }

        @Override
        void store(DataMap dataMap, String key, DataMap value) {
            dataMap.putDataMap(key, value);
        }

        @Override
        void storeList(DataMap dataMap, String key, ArrayList<DataMap> valueList) {
            dataMap.putDataMapArrayList(key, valueList);
        }

        @Override
        DataMap load(DataMap dataMap, String key) {
            return dataMap.getDataMap(key);
        }

        @Override
        AnnotatedArrayList<DataMap> loadList(DataMap dataMap, String key) {
            AnnotatedArrayList<DataMap> list = new AnnotatedArrayList<DataMap>(this);
            list.addAll(dataMap.getDataMapArrayList(key));
            return list;
        }
    };

    public static final int LIST_TYPE_CODE = 10;
    static TypeHelper<AnnotatedArrayList<?>> ARRAYLIST = new TypeHelper<AnnotatedArrayList<?>>(LIST_TYPE_CODE) {
        @Override
        AnnotatedArrayList read(DataBundleValue value, List<Asset> assets) {
            TypeHelper innerTypeHelper = NULL;
            for (DataBundleTypedValue typedValue : value.list) {
                if (innerTypeHelper == NULL) {
                    innerTypeHelper = getTypeHelper(typedValue.type);
                } else if (typedValue.type != innerTypeHelper.type && typedValue.type != NULL.type) {
                    throw new IllegalArgumentException("List has elements of different types: " + innerTypeHelper.type + " and " + typedValue.type);
                }
            }
            return innerTypeHelper.readList(value.list, assets);
        }

        @Override
        DataBundleValue create(AnnotatedArrayList<?> value, List<Asset> assets) {
            return new DataBundleValue.Builder().list(value.createList(assets)).build();
        }

        @Override
        void store(DataMap dataMap, String key, AnnotatedArrayList value) {
            value.store(dataMap, key);
        }

        @Override
        AnnotatedArrayList load(DataMap dataMap, String key) {
            return getListInnerTypeHelper(dataMap.getType(key)).loadList(dataMap, key);
        }
    };

    public static final int STRING_ARRAY_TYPE_CODE = 11;
    static TypeHelper<String[]> STRINGARRAY = new TypeHelper<String[]>(STRING_ARRAY_TYPE_CODE) {
        @Override
        String[] read(DataBundleValue value, List<Asset> assets) {
            return value.stringArray.toArray(new String[value.stringArray.size()]);
        }

        @Override
        DataBundleValue create(String[] value, List<Asset> assets) {
            return new DataBundleValue.Builder().stringArray(Arrays.asList(value)).build();
        }

        @Override
        void store(DataMap dataMap, String key, String[] value) {
            dataMap.putStringArray(key, value);
        }

        @Override
        String[] load(DataMap dataMap, String key) {
            return dataMap.getStringArray(key);
        }
    };

    public static final int LONG_ARRAY_TYPE_CODE = 12;
    static TypeHelper<long[]> LONGARRAY = new TypeHelper<long[]>(LONG_ARRAY_TYPE_CODE) {
        @Override
        long[] read(DataBundleValue value, List<Asset> assets) {
            long[] longArr = new long[value.longArray.size()];
            for (int i = 0; i < value.longArray.size(); i++) {
                longArr[i] = value.longArray.get(i);
            }
            return longArr;
        }

        @Override
        DataBundleValue create(long[] value, List<Asset> assets) {
            List<Long> longList = new ArrayList<Long>(value.length);
            for (long l : value) {
                longList.add(l);
            }
            return new DataBundleValue.Builder().longArray(longList).build();
        }

        @Override
        void store(DataMap dataMap, String key, long[] value) {
            dataMap.putLongArray(key, value);
        }

        @Override
        long[] load(DataMap dataMap, String key) {
            return dataMap.getLongArray(key);
        }
    };

    public static final int ASSET_TYPE_CODE = 13;
    static TypeHelper<Asset> ASSET = new TypeHelper<Asset>(ASSET_TYPE_CODE) {
        @Override
        Asset read(DataBundleValue value, List<Asset> assets) {
            return assets.get(value.assetIndex);
        }

        @Override
        DataBundleValue create(Asset value, List<Asset> assets) {
            int index;
            if (assets.contains(value)) {
                index = assets.indexOf(value);
            } else {
                index = assets.size();
                assets.add(value);
            }
            return new DataBundleValue.Builder().assetIndex(index).build();
        }

        @Override
        void store(DataMap dataMap, String key, Asset value) {
            dataMap.putAsset(key, value);
        }

        @Override
        Asset load(DataMap dataMap, String key) {
            return dataMap.getAsset(key);
        }
    };

    public static final int NULL_TYPE_CODE = 14;
    static TypeHelper<String> NULL = new TypeHelper<String>(NULL_TYPE_CODE) {
        @Override
        String read(DataBundleValue value, List<Asset> assets) {
            return null;
        }

        @Override
        DataBundleValue create(String value, List<Asset> assets) {
            return new DataBundleValue.Builder().build();
        }

        @Override
        void store(DataMap dataMap, String key, String value) {
            dataMap.putString(key, value);
        }

        @Override
        void storeList(DataMap dataMap, String key, ArrayList<String> valueList) {
            dataMap.putStringArrayList(key, valueList);
        }

        @Override
        String load(DataMap dataMap, String key) {
            return null;
        }

        @Override
        AnnotatedArrayList<String> loadList(DataMap dataMap, String key) {
            AnnotatedArrayList<String> list = new AnnotatedArrayList<String>(this);
            list.addAll(dataMap.getStringArrayList(key));
            return list;
        }
    };

    public static final int FLOAT_ARRAY_TYPE_CODE = 15;
    static TypeHelper<float[]> FLOATARRAY = new TypeHelper<float[]>(FLOAT_ARRAY_TYPE_CODE) {
        @Override
        float[] read(DataBundleValue value, List<Asset> assets) {
            float[] floatArr = new float[value.floatArray.size()];
            for (int i = 0; i < value.floatArray.size(); i++) {
                floatArr[i] = value.floatArray.get(i);
            }
            return floatArr;
        }

        @Override
        DataBundleValue create(float[] value, List<Asset> assets) {
            List<Float> floatList = new ArrayList<Float>(value.length);
            for (float f : value) {
                floatList.add(f);
            }
            return new DataBundleValue.Builder().floatArray(floatList).build();
        }

        @Override
        void store(DataMap dataMap, String key, float[] value) {
            dataMap.putFloatArray(key, value);
        }

        @Override
        float[] load(DataMap dataMap, String key) {
            return dataMap.getFloatArray(key);
        }
    };

    static {
        typeHelperByCode = new SparseArray<TypeHelper>();
        rememberTypeReader(BYTEARRAY);
        rememberTypeReader(STRING);
        rememberTypeReader(DOUBLE);
        rememberTypeReader(FLOAT);
        rememberTypeReader(LONG);
        rememberTypeReader(INTEGER);
        rememberTypeReader(BYTE);
        rememberTypeReader(BOOLEAN);
        rememberTypeReader(DATAMAP);
        rememberTypeReader(ARRAYLIST);
        rememberTypeReader(STRINGARRAY);
        rememberTypeReader(LONGARRAY);
        rememberTypeReader(ASSET);
        rememberTypeReader(NULL);
        rememberTypeReader(FLOATARRAY);
    }

    static class AssetAnnotatedDataBundle {
        private DataBundle dataBundle;
        private List<Asset> assets;

        public List<Asset> getAssets() {
            return assets;
        }

        public byte[] getData() {
            return dataBundle.encode();
        }
    }

    static class AnnotatedArrayList<T> extends ArrayList<T> {
        private TypeHelper<T> innerType;

        public AnnotatedArrayList(TypeHelper<T> innerType) {
            this.innerType = innerType;
        }

        void store(DataMap dataMap, String key) {
            innerType.storeList(dataMap, key, this);
        }

        public List<DataBundleTypedValue> createList(List<Asset> assets) {
            return innerType.createList(this, assets);
        }
    }

    static abstract class TypeHelper<T> {
        private int type;

        public TypeHelper(int type) {
            this.type = type;
        }

        abstract T read(DataBundleValue value, List<Asset> assets);

        abstract DataBundleValue create(T value, List<Asset> assets);

        T read(DataBundleTypedValue value, List<Asset> assets) {
            if (value.type == NULL_TYPE_CODE) {
                return null;
            } else if (value.type == type) {
                return read(value.value_, assets);
            } else {
                throw new IllegalArgumentException();
            }
        }

        abstract void store(DataMap dataMap, String key, T value);

        void storeList(DataMap dataMap, String key, ArrayList<T> valueList) {
            throw new UnsupportedOperationException();
        }

        abstract T load(DataMap dataMap, String key);

        AnnotatedArrayList<T> loadList(DataMap dataMap, String key) {
            throw new UnsupportedOperationException();
        }

        void readAndStore(DataMap dataMap, String key, DataBundleValue value, List<Asset> assets) {
            store(dataMap, key, read(value, assets));
        }

        void readAndStore(DataMap dataMap, String key, DataBundleTypedValue value, List<Asset> assets) {
            store(dataMap, key, read(value, assets));
        }

        void readAndStore(DataMap dataMap, DataBundleEntry entry, List<Asset> assets) {
            readAndStore(dataMap, entry.key, entry.typedValue, assets);
        }

        AnnotatedArrayList<T> readList(List<DataBundleTypedValue> values, List<Asset> assets) {
            AnnotatedArrayList<T> list = new AnnotatedArrayList<T>(this);
            for (DataBundleTypedValue value : values) {
                list.add(read(value, assets));
            }
            return list;
        }

        List<DataBundleTypedValue> createList(AnnotatedArrayList<T> value, List<Asset> assets) {
            List<DataBundleTypedValue> list = new ArrayList<DataBundleTypedValue>();
            for (T val : value) {
                list.add(createTyped(val, assets));
            }
            return list;
        }

        void readAndStore(DataMap dataMap, String key, List<DataBundleTypedValue> values, List<Asset> assets) {
            storeList(dataMap, key, readList(values, assets));
        }

        DataBundleTypedValue createTyped(T value, List<Asset> assets) {
            return new DataBundleTypedValue.Builder().type(type).value_(create(value, assets)).build();
        }

        DataBundleValue loadAndCreate(DataMap dataMap, String key, List<Asset> assets) {
            return create(load(dataMap, key), assets);
        }

        DataBundleTypedValue loadAndCreateTyped(DataMap dataMap, String key, List<Asset> assets) {
            return createTyped(load(dataMap, key), assets);
        }

        DataBundleEntry loadAndCreateEntry(DataMap dataMap, String key, List<Asset> assets) {
            return new DataBundleEntry.Builder().key(key).typedValue(loadAndCreateTyped(dataMap, key, assets)).build();
        }
    }
}
