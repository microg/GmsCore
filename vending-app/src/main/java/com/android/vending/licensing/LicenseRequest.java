package com.android.vending.licensing;

import static com.android.volley.Request.Method.GET;

import android.util.Base64;
import android.util.Log;

import com.android.vending.AndroidVersionMeta;
import com.android.vending.DeviceMeta;
import com.android.vending.EncodedTriple;
import com.android.vending.EncodedTripleWrapper;
import com.android.vending.IntWrapper;
import com.android.vending.LicenseRequestHeader;
import com.android.vending.LicenseResult;
import com.android.vending.Locality;
import com.android.vending.LocalityWrapper;
import com.android.vending.StringWrapper;
import com.android.vending.Timestamp;
import com.android.vending.TimestampContainer;
import com.android.vending.TimestampContainer1;
import com.android.vending.TimestampContainer1Wrapper;
import com.android.vending.TimestampContainer2;
import com.android.vending.TimestampStringWrapper;
import com.android.vending.TimestampWrapper;
import com.android.vending.UnknownByte12;
import com.android.vending.UserAgent;
import com.android.vending.Util;
import com.android.vending.Uuid;
import com.android.vending.V1Container;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.BuildConfig;

import org.microg.gms.profile.Build;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import okio.ByteString;

public abstract class LicenseRequest<T> extends Request<T> {

    private final String auth;
    private static final String TAG = "FakeLicenseRequest";

    private static final int BASE64_FLAGS = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;
    long ANDROID_ID = 1;
    private static final String FINSKY_VERSION = "Finsky/37.5.24-29%20%5B0%5D%20%5BPR%5D%20565477504";

    private final Response.Listener<T> successListener;


    protected LicenseRequest(String url, String auth, Response.Listener<T> successListener, Response.ErrorListener errorListener) {
        super(GET, url, errorListener);
        this.auth = auth;

        this.successListener = successListener;
    }

    @Override
    public Map<String, String> getHeaders() {

        long millis = System.currentTimeMillis();
        TimestampContainer.Builder timestamp = new TimestampContainer.Builder()
            .container2(new TimestampContainer2.Builder()
                .wrapper(new TimestampWrapper.Builder().timestamp(makeTimestamp(millis)).build())
                .timestamp(makeTimestamp(millis))
                .build());
        millis = System.currentTimeMillis();
        timestamp
            .container1Wrapper(new TimestampContainer1Wrapper.Builder()
                .androidId(String.valueOf(ANDROID_ID))
                .container(new TimestampContainer1.Builder()
                    .timestamp(millis + "000")
                    .wrapper(makeTimestamp(millis))
                    .build())
                .build()
            );
        String encodedTimestamps = new String(
            Base64.encode(Util.encodeGzip(timestamp.build().encode()), BASE64_FLAGS)
        );

        Locality locality = new Locality.Builder()
            .unknown1(1)
            .unknown2(2)
            .countryCode("")
            .region(new TimestampStringWrapper.Builder()
                .string("").timestamp(makeTimestamp(System.currentTimeMillis())).build())
            .country(new TimestampStringWrapper.Builder()
                .string("").timestamp(makeTimestamp(System.currentTimeMillis())).build())
            .unknown3(0)
            .build();
        String encodedLocality = new String(
            Base64.encode(locality.encode(), BASE64_FLAGS)
        );

        byte[] header = new LicenseRequestHeader.Builder()
            .encodedTimestamps(new StringWrapper.Builder().string(encodedTimestamps).build())
            .triple(
                new EncodedTripleWrapper.Builder().triple(
                    new EncodedTriple.Builder()
                        .encoded1("")
                        .encoded2("")
                        .empty("")
                        .build()
                ).build()
            )
            .locality(new LocalityWrapper.Builder().encodedLocalityProto(encodedLocality).build())
            .unknown(new IntWrapper.Builder().integer(5).build())
            .empty("")
            .deviceMeta(new DeviceMeta.Builder()
                .android(
                    new AndroidVersionMeta.Builder()
                        .androidSdk(Build.VERSION.SDK_INT)
                        .buildNumber(Build.ID)
                        .androidVersion(Build.VERSION.RELEASE)
                        .unknown(0)
                        .build()
                )
                .unknown1(new UnknownByte12.Builder().bytes(new ByteString(new byte[]{}
                )).build())
                .unknown2(1)
                .build()
            )
            .userAgent(new UserAgent.Builder()
                .deviceName(Build.DEVICE)
                .deviceHardware(Build.HARDWARE)
                .deviceModelName(Build.MODEL)
                .finskyVersion(FINSKY_VERSION)
                .deviceProductName(Build.MODEL)
                .androidId(ANDROID_ID) // must not be 0
                .buildFingerprint(Build.FINGERPRINT)
                .build()
            )
            .uuid(new Uuid.Builder()
                .uuid(UUID.randomUUID().toString())
                .unknown(2)
                .build()
            )
            .build().encode();
        String xPsRh = new String(Base64.encode(Util.encodeGzip(header), BASE64_FLAGS));

        Log.v(TAG, "X-PS-RH: " + xPsRh);

        String userAgent = FINSKY_VERSION + " (api=3,versionCode=" + BuildConfig.VERSION_CODE + ",sdk=" + Build.VERSION.SDK +
            ",device=" + encodeString(Build.DEVICE) + ",hardware=" + encodeString(Build.HARDWARE) + ",product=" + encodeString(Build.PRODUCT) +
            ",platformVersionRelease=" + encodeString(Build.VERSION.RELEASE) + ",model=" + encodeString(Build.MODEL) + ",buildId=" + encodeString(Build.ID) +
            ",isWideScreen=" + 0 + ",supportedAbis=" + String.join(";", Build.SUPPORTED_ABIS) + ")";
        Log.v(TAG, "User-Agent: " + userAgent);

        return Map.of(
            "X-PS-RH", xPsRh,
            "User-Agent", userAgent,
            "Authorization", "Bearer " + auth,
            "Accept-Language", "en-US",
            "Connection", "Keep-Alive"
        );
    }

    private static String encodeString(String s) {
        return URLEncoder.encode(s).replace("+", "%20");
    }

    @Override
    protected void deliverResponse(T response) {
        successListener.onResponse(response);
    }

    private static Timestamp makeTimestamp(long millis) {
        return new Timestamp.Builder()
            .seconds((millis / 1000))
            .nanos(Math.floorMod(millis, 1000) * 1000000)
            .build();
    }

    public static class V1 extends LicenseRequest<V1Container> {

        public V1(String packageName, String auth, int versionCode, long nonce, Response.Listener<V1Container> successListener, Response.ErrorListener errorListener) {
            super("https://play-fe.googleapis.com/fdfe/apps/checkLicense?pkgn=" + packageName + "&vc=" + versionCode + "&nnc=" + nonce,
                auth, successListener, errorListener
            );
        }

        @Override
        protected Response<V1Container> parseNetworkResponse(NetworkResponse response) {
            if (response != null && response.data != null) {
                try {
                    LicenseResult result = LicenseResult.ADAPTER.decode(response.data);
                    return Response.success(result.information.v1, null);
                } catch (IOException e) {
                    return Response.error(new VolleyError(e));
                } catch (NullPointerException e) {
                    // A field does not exist → user has no license
                    return Response.success(null, null);
                }
            } else {
                return Response.error(new VolleyError("No response was returned"));
            }
        }
    }

    public static class V2 extends LicenseRequest<String> {
        public V2(String packageName, String auth, int versionCode, Response.Listener<String> successListener,
                  Response.ErrorListener errorListener) {
            super(
                "https://play-fe.googleapis.com/fdfe/apps/checkLicenseServerFallback?pkgn=" + packageName + "&vc=" + versionCode,
                auth, successListener, errorListener
            );
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            if (response != null && response.data != null) {
                try {
                    LicenseResult result = LicenseResult.ADAPTER.decode(response.data);
                    return Response.success(result.information.v2.license.jwt, null);
                } catch (IOException e) {
                    return Response.error(new VolleyError(e));
                } catch (NullPointerException e) {
                    // A field does not exist → user has no license
                    return Response.success(null, null);
                }
            } else {
                return Response.error(new VolleyError("No response was returned"));
            }
        }
    }
}
