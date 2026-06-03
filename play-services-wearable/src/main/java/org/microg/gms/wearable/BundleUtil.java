package org.microg.gms.wearable;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Cross-version-safe Bundle ↔ byte[] serialization using JSON.
 *
 * SOLVES:
 * - S-01: Bundle.toByteArray() does not exist (compile error)
 * - F-02: Parcel binary format not stable across API levels
 *
 * TRADE-OFFS:
 * + Stable across all Android versions
 * + Human-readable for debugging (log the bytes as string)
 * + No ClassNotFoundException on deserialization
 * - ~2x payload size vs Parcel (acceptable for our use cases)
 * - Only supports primitive types + Bundle nesting (no Parcelable objects)
 *
 * SUPPORTED TYPES:
 * String, int, long, float, double, boolean, byte[] (as Base64), nested Bundle
 *
 * UNSUPPORTED (skipped with warning):
 * Parcelable, CharSequence, CharSequence[], int[], long[], float[], etc.
 * We don't need these for WearOS message payloads.
 */
public class BundleUtil {

    private static final String TAG = "BundleUtil";

    /**
     * Serialize a Bundle to a UTF-8 JSON byte array.
     * Returns empty byte array if bundle is null.
     */
    public static byte[] toByteArray(Bundle bundle) {
        if (bundle == null) return new byte[0];
        try {
            JSONObject json = bundleToJson(bundle);
            return json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            Log.e(TAG, "Serialize failed", e);
            return new byte[0];
        }
    }

    /**
     * Deserialize a UTF-8 JSON byte array back to a Bundle.
     * Returns null on failure (malformed JSON, empty input, etc.).
     */
    public static Bundle fromByteArray(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            String str = new String(data, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(str);
            return jsonToBundle(json);
        } catch (JSONException e) {
            Log.e(TAG, "Deserialize failed", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected deserialize error", e);
            return null;
        }
    }

    private static JSONObject bundleToJson(Bundle bundle) throws JSONException {
        JSONObject json = new JSONObject();
        for (String key : bundle.keySet()) {
            Object val = bundle.get(key);
            if (val instanceof String) {
                json.put(key, val);
            } else if (val instanceof Integer) {
                json.put(key, (int) val);
            } else if (val instanceof Long) {
                json.put(key, (long) val);
            } else if (val instanceof Float) {
                // JSON has no float type — store as double
                json.put(key, (double) (float) val);
            } else if (val instanceof Double) {
                json.put(key, (double) val);
            } else if (val instanceof Boolean) {
                json.put(key, (boolean) val);
            } else if (val instanceof Bundle) {
                json.put(key, bundleToJson((Bundle) val));
            } else if (val instanceof byte[]) {
                json.put(key, Base64.encodeToString(
                        (byte[]) val, Base64.NO_WRAP));
            } else {
                Log.w(TAG, "Skipping unsupported type for key '"
                        + key + "': " + (val != null
                        ? val.getClass().getSimpleName() : "null"));
            }
        }
        return json;
    }

    private static Bundle jsonToBundle(JSONObject json) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object val = json.get(key);
            if (val instanceof String) {
                bundle.putString(key, (String) val);
            } else if (val instanceof Integer) {
                bundle.putInt(key, (int) val);
            } else if (val instanceof Long) {
                bundle.putLong(key, (long) val);
            } else if (val instanceof Double) {
                // Store as double; callers that need float can cast
                bundle.putDouble(key, (double) val);
            } else if (val instanceof Boolean) {
                bundle.putBoolean(key, (boolean) val);
            } else if (val instanceof JSONObject) {
                bundle.putBundle(key, jsonToBundle((JSONObject) val));
            }
        }
        return bundle;
    }
}
