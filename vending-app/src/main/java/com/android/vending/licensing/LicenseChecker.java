package com.android.vending.licensing;

import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.os.Binder.getCallingUid;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.V1Container;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import java.io.IOException;

import kotlin.Unit;

/**
 * Performs license check including caller UID verification, using a given account, for which
 * an auth token is fetched.
 *
 * @param <D> Request parameter data value type
 * @param <R> Result type
 */
public abstract class LicenseChecker<D, R> {

    private static final String TAG = "FakeLicenseChecker";

    /* Possible response codes for checkLicense v1, from
     * https://developer.android.com/google/play/licensing/licensing-reference#server-response-codes and
     * the LVL library.
     */

    /**
     * The application is licensed to the user. The user has purchased the application, or is authorized to
     * download and install the alpha or beta version of the application.
     */
    static final int LICENSED = 0x0;
    /**
     * The application is not licensed to the user.
     */
    static final int NOT_LICENSED = 0x1;
    /**
     * The application is licensed to the user, but there is an updated application version available that is
     * signed with a different key.
     */
    static final int LICENSED_OLD_KEY = 0x2;
    /**
     * Server error — the application (package name) was not recognized by Google Play.
     */
    static final int ERROR_NOT_MARKET_MANAGED = 0x3;
    /**
     * Server error — the server could not load the application's key pair for licensing.
     */
    static final int ERROR_SERVER_FAILURE = 0x4;
    static final int ERROR_OVER_QUOTA = 0x5;

    /**
     * Local error — the Google Play application was not able to reach the licensing server, possibly because
     * of network availability problems.
     */
    static final int ERROR_CONTACTING_SERVER = 0x101;
    /**
     * Local error — the application requested a license check for a package that is not installed on the device.
     */
    static final int ERROR_INVALID_PACKAGE_NAME = 0x102;
    /**
     * Local error — the application requested a license check for a package whose UID (package, user ID pair)
     * does not match that of the requesting application.
     */
    static final int ERROR_NON_MATCHING_UID = 0x103;

    static final String AUTH_TOKEN_SCOPE = "oauth2:https://www.googleapis.com/auth/googleplay";

    public abstract LicenseRequest<?> createRequest(String packageName, String auth, int versionCode, D data,
                                             BiConsumer<Integer, R> then, Response.ErrorListener errorListener);

    public void checkLicense(Account account, AccountManager accountManager, String androidId,
                             String packageName, int callingUid, PackageManager packageManager,
                             RequestQueue queue, D queryData,
                             BiConsumerWithException<Integer, R, RemoteException> onResult)
        throws RemoteException {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            int versionCode = packageInfo.versionCode;

            // Verify caller identity
            if (packageInfo.applicationInfo.uid != callingUid) {
                Log.e(TAG,
                    "an app illegally tried to request licenses for another app (caller: " + callingUid + ")");
                safeSendResult(onResult, ERROR_NON_MATCHING_UID, null);
            } else {

                BiConsumer<Integer, R> onRequestFinished = (Integer integer, R r) -> {
                    try {
                        onResult.accept(integer, r);
                    } catch (RemoteException e) {
                        Log.e(TAG,
                            "After telling it the license check result, remote threw an Exception.");
                        e.printStackTrace();
                    }
                };

                Response.ErrorListener onRequestError = error -> {
                    Log.e(TAG, "license request failed with " + error.toString());
                    safeSendResult(onResult, ERROR_CONTACTING_SERVER, null);
                };

                accountManager.getAuthToken(
                    account, AUTH_TOKEN_SCOPE, false,
                    future -> {
                        try {
                            String auth = future.getResult().getString(KEY_AUTHTOKEN);
                            LicenseRequest<?> request = createRequest(packageName, auth,
                                versionCode, queryData, onRequestFinished, onRequestError);

                            if (androidId != null) {
                                request.ANDROID_ID = Long.parseLong(androidId, 16);
                            }

                            request.setShouldCache(false);
                            queue.add(request);
                        } catch (AuthenticatorException | IOException | OperationCanceledException e) {
                            safeSendResult(onResult, ERROR_CONTACTING_SERVER, null);
                        }

                    }, null);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an app tried to request licenses for package " + packageName + ", which does not exist");
            onResult.accept(ERROR_INVALID_PACKAGE_NAME, null);
        }
    }

    private static <A, B, T extends Exception> void safeSendResult(
        BiConsumerWithException<A, B, T> consumerWithException, A a, B b) {
        try {
            consumerWithException.accept(a, b);
        } catch (Exception e) {
            Log.e(TAG, "While sending result " + a + ", " + b + ", remote encountered an exception.");
            e.printStackTrace();
        }
    }

    // Implementations

    public static class V1 extends LicenseChecker<Long, Tuple<String, String>> {

        @Override
        public LicenseRequest<V1Container> createRequest(String packageName, String auth, int versionCode, Long nonce, BiConsumer<Integer, Tuple<String, String>> then,
                                                  Response.ErrorListener errorListener) {
            return new LicenseRequest.V1(
                packageName, auth, versionCode, nonce, response -> {
                if (response != null) {
                    Log.v(TAG, "licenseV1 result was " + response.result + " with signed data " +
                        response.signedData);

                    if (response.result != null) {
                        then.accept(response.result, new Tuple<>(response.signedData, response.signature));
                    } else {
                        then.accept(LICENSED, new Tuple<>(response.signedData, response.signature));
                    }
                }
            }, errorListener
            );
        }
    }

    public static class V2 extends LicenseChecker<Unit, String> {
        @Override
        public LicenseRequest<String> createRequest(String packageName, String auth, int versionCode, Unit data,
                                             BiConsumer<Integer, String> then, Response.ErrorListener errorListener) {
            return new LicenseRequest.V2(
                packageName, auth, versionCode, response -> {
                if (response != null) {
                    then.accept(LICENSED, response);
                } else {
                    then.accept(NOT_LICENSED, null);
                }
            }, errorListener
            );
        }
    }

    // Functional interfaces

    interface BiConsumerWithException<A, B, T extends Exception> {
        void accept(A a, B b) throws T;
    }

    interface BiConsumer<A, B> {
        void accept(A a, B b);
    }

    static class Tuple<A, B> {
        public final A a;
        public final B b;

        public Tuple(A a, B b) {
            this.a = a;
            this.b = b;
        }
    }
}
