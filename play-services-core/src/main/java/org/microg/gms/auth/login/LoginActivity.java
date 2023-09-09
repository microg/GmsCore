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

package org.microg.gms.auth.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.webkit.WebViewClientCompat;

import com.google.android.gms.R;

import org.json.JSONArray;
import org.microg.gms.auth.AuthConstants;
import org.microg.gms.auth.AuthManager;
import org.microg.gms.auth.AuthRequest;
import org.microg.gms.auth.AuthResponse;
import org.microg.gms.checkin.CheckinManager;
import org.microg.gms.checkin.LastCheckinInfo;
import org.microg.gms.common.HttpFormClient;
import org.microg.gms.common.Utils;
import org.microg.gms.people.PeopleManager;
import org.microg.gms.profile.Build;
import org.microg.gms.profile.ProfileManager;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Locale;

import static android.accounts.AccountManager.PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE;
import static android.accounts.AccountManager.VISIBILITY_USER_MANAGED_VISIBLE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.GINGERBREAD_MR1;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT;
import static org.microg.gms.auth.AuthPrefs.isAuthVisible;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.common.Constants.GMS_VERSION_CODE;

public class LoginActivity extends AssistantActivity {
    public static final String TMPL_NEW_ACCOUNT = "new_account";
    public static final String EXTRA_TMPL = "tmpl";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_TOKEN = "masterToken";
    public static final int STATUS_BAR_DISABLE_BACK = 0x00400000;

    private static final String TAG = "GmsAuthLoginBrowser";
    private static final String EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup";
    private static final String PROGRAMMATIC_AUTH_URL = "https://accounts.google.com/o/oauth2/programmatic_auth";
    private static final String GOOGLE_SUITE_URL = "https://accounts.google.com/signin/continue";
    private static final String MAGIC_USER_AGENT = " MinuteMaid";
    private static final String COOKIE_OAUTH_TOKEN = "oauth_token";

    private final FidoHandler fidoHandler = new FidoHandler(this);
    private final DroidGuardHandler dgHandler = new DroidGuardHandler(this);

    private WebView webView;
    private String accountType;
    private AccountManager accountManager;
    private InputMethodManager inputMethodManager;
    private ViewGroup authContent;
    private int state = 0;

    @SuppressLint("AddJavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountType = AuthConstants.DEFAULT_ACCOUNT_TYPE;
        accountManager = AccountManager.get(LoginActivity.this);
        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        webView = createWebView(this);
        webView.addJavascriptInterface(new JsBridge(), "mm");
        authContent = (ViewGroup) findViewById(R.id.auth_content);
        ((ViewGroup) findViewById(R.id.auth_root)).addView(webView);
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "pageFinished: " + view.getUrl());
                Uri uri = Uri.parse(view.getUrl());

                // Begin login.
                // Only required if client code does not invoke showView() via JSBridge
                if ("identifier".equals(uri.getFragment()) || uri.getPath().endsWith("/identifier"))
                    runOnUiThread(() -> webView.setVisibility(VISIBLE));

                // Normal login.
                if ("close".equals(uri.getFragment()))
                    closeWeb(false);

                // Google Suite login.
                if (url.startsWith(GOOGLE_SUITE_URL))
                    closeWeb(false);

                // IDK when this is called.
                if (url.startsWith(PROGRAMMATIC_AUTH_URL))
                    closeWeb(true);
            }
        });
        if (getIntent().hasExtra(EXTRA_TOKEN)) {
            if (getIntent().hasExtra(EXTRA_EMAIL)) {
                AccountManager accountManager = AccountManager.get(this);
                Account account = new Account(getIntent().getStringExtra(EXTRA_EMAIL), accountType);
                accountManager.addAccountExplicitly(account, getIntent().getStringExtra(EXTRA_TOKEN), null);
                if (isAuthVisible(this) && SDK_INT >= 26) {
                    accountManager.setAccountVisibility(account, PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE, VISIBILITY_USER_MANAGED_VISIBLE);
                }
                retrieveGmsToken(account);
            } else {
                retrieveRtToken(getIntent().getStringExtra(EXTRA_TOKEN));
            }
        } else if (android.os.Build.VERSION.SDK_INT < 21) {
            init();
        } else {
            setMessage(R.string.auth_before_connect);
            setBackButtonText(android.R.string.cancel);
            setNextButtonText(R.string.auth_sign_in);
        }
    }

    @Override
    protected void onNextButtonClicked() {
        super.onNextButtonClicked();
        state++;
        if (state == 1) {
            init();
        } else if (state == -1) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onBackButtonClicked() {
        super.onBackButtonClicked();
        state--;
        if (state == -1) {
            finish();
        }
    }

    private void init() {
        setTitle(R.string.just_a_sec);
        setBackButtonText(null);
        setNextButtonText(null);
        View loading = getLayoutInflater().inflate(R.layout.login_assistant_loading, authContent, false);
        authContent.removeAllViews();
        authContent.addView(loading);
        setMessage(R.string.auth_connecting);
        CookieManager.getInstance().setAcceptCookie(true);
        if (SDK_INT >= LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(value -> start());
        } else {
            //noinspection deprecation
            CookieManager.getInstance().removeAllCookie();
            start();
        }
    }

    private static WebView createWebView(Context context) {
        WebView webView = new WebView(context);
        if (SDK_INT < LOLLIPOP) {
            webView.setVisibility(VISIBLE);
        } else {
            webView.setVisibility(INVISIBLE);
        }
        webView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.TRANSPARENT);
        prepareWebViewSettings(context, webView.getSettings());
        return webView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void prepareWebViewSettings(Context context, WebSettings settings) {
        ProfileManager.ensureInitialized(context);
        settings.setUserAgentString(Build.INSTANCE.generateWebViewUserAgentString(settings.getUserAgentString()) + MAGIC_USER_AGENT);
        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setSaveFormData(false);
        settings.setAllowFileAccess(false);
        settings.setDatabaseEnabled(false);
        settings.setNeedInitialFocus(false);
        settings.setUseWideViewPort(false);
        settings.setSupportZoom(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
    }

    private void start() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (LastCheckinInfo.read(this).getAndroidId() == 0) {
                new Thread(() -> {
                    Runnable next;
                    next = checkin(false) ? this::loadLoginPage : () -> showError(R.string.auth_general_error_desc);
                    LoginActivity.this.runOnUiThread(next);
                }).start();
            } else {
                loadLoginPage();
            }
        } else {
            showError(R.string.no_network_error_desc);
        }
    }

    private void showError(int errorRes) {
        setTitle(R.string.sorry);
        findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
        setMessage(errorRes);
    }

    private void setMessage(@StringRes int res) {
        setMessage(getText(res));
    }

    private void setMessage(CharSequence text) {
        ((TextView) findViewById(R.id.description_text)).setText(text);
    }

    private void loadLoginPage() {
        String tmpl = getIntent().hasExtra(EXTRA_TMPL) ? getIntent().getStringExtra(EXTRA_TMPL) : TMPL_NEW_ACCOUNT;
        webView.loadUrl(buildUrl(tmpl, Utils.getLocale(this)));
    }

    protected void runScript(String js) {
        runOnUiThread(() -> webView.loadUrl("javascript:" + js));
    }

    private void closeWeb(boolean programmaticAuth) {
        setMessage(R.string.auth_finalize);
        runOnUiThread(() -> webView.setVisibility(INVISIBLE));
        String cookies = CookieManager.getInstance().getCookie(programmaticAuth ? PROGRAMMATIC_AUTH_URL : EMBEDDED_SETUP_URL);
        String[] temp = cookies.split(";");
        for (String ar1 : temp) {
            if (ar1.trim().startsWith(COOKIE_OAUTH_TOKEN + "=")) {
                String[] temp1 = ar1.split("=");
                retrieveRtToken(temp1[1]);
                return;
            }
        }
        showError(R.string.auth_general_error_desc);
    }

    private void retrieveRtToken(String oAuthToken) {
        new AuthRequest().fromContext(this)
                .appIsGms()
                .callerIsGms()
                .service("ac2dm")
                .token(oAuthToken).isAccessToken()
                .addAccount()
                .getAccountId()
                .droidguardResults(null /*TODO*/)
                .getResponseAsync(new HttpFormClient.Callback<AuthResponse>() {
                    @Override
                    public void onResponse(AuthResponse response) {
                        Account account = new Account(response.email, accountType);
                        if (accountManager.addAccountExplicitly(account, response.token, null)) {
                            accountManager.setAuthToken(account, "SID", response.Sid);
                            accountManager.setAuthToken(account, "LSID", response.LSid);
                            accountManager.setUserData(account, "flags", "1");
                            accountManager.setUserData(account, "services", response.services);
                            accountManager.setUserData(account, "oauthAccessToken", "1");
                            accountManager.setUserData(account, "firstName", response.firstName);
                            accountManager.setUserData(account, "lastName", response.lastName);
                            if (!TextUtils.isEmpty(response.accountId))
                                accountManager.setUserData(account, "GoogleUserId", response.accountId);

                            retrieveGmsToken(account);
                            setResult(RESULT_OK);
                        } else {
                            Log.w(TAG, "Account NOT created!");
                            runOnUiThread(() -> {
                                showError(R.string.auth_general_error_desc);
                                setNextButtonText(android.R.string.ok);
                            });
                            state = -2;
                        }
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException", exception);
                        runOnUiThread(() -> {
                            showError(R.string.auth_general_error_desc);
                            setNextButtonText(android.R.string.ok);
                        });
                        state = -2;
                    }
                });
    }

    private void retrieveGmsToken(final Account account) {
        final AuthManager authManager = new AuthManager(this, account.name, GMS_PACKAGE_NAME, "ac2dm");
        authManager.setPermitted(true);
        new AuthRequest().fromContext(this)
                .appIsGms()
                .callerIsGms()
                .service(authManager.getService())
                .email(account.name)
                .token(AccountManager.get(this).getPassword(account))
                .systemPartition(true)
                .hasPermission(true)
                .addAccount()
                .getAccountId()
                .getResponseAsync(new HttpFormClient.Callback<AuthResponse>() {
                    @Override
                    public void onResponse(AuthResponse response) {
                        authManager.storeResponse(response);
                        String accountId = PeopleManager.loadUserInfo(LoginActivity.this, account);
                        if (!TextUtils.isEmpty(accountId))
                            accountManager.setUserData(account, "GoogleUserId", accountId);
                        checkin(true);
                        finish();
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException", exception);
                        runOnUiThread(() -> {
                            showError(R.string.auth_general_error_desc);
                            setNextButtonText(android.R.string.ok);
                        });
                        state = -2;
                    }
                });
    }

    private boolean checkin(boolean force) {
        try {
            CheckinManager.checkin(LoginActivity.this, force);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Checkin failed", e);
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KEYCODE_BACK) && webView.canGoBack() && (webView.getVisibility() == VISIBLE)) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static String buildUrl(String tmpl, Locale locale) {
        return Uri.parse(EMBEDDED_SETUP_URL).buildUpon()
                .appendQueryParameter("source", "android")
                .appendQueryParameter("xoauth_display_name", "Android Device")
                .appendQueryParameter("lang", locale.getLanguage())
                .appendQueryParameter("cc", locale.getCountry().toLowerCase(Locale.US))
                .appendQueryParameter("langCountry", locale.toString().toLowerCase(Locale.US))
                .appendQueryParameter("hl", locale.toString().replace("_", "-"))
                .appendQueryParameter("tmpl", tmpl)
                .build().toString();
    }

    private class JsBridge {

        @JavascriptInterface
        public final void addAccount(String json) {
            Log.d(TAG, "JSBridge: addAccount");
        }

        @JavascriptInterface
        public final void attemptLogin(String accountName, String password) {
            Log.d(TAG, "JSBridge: attemptLogin");
        }

        @JavascriptInterface
        public void backupSyncOptIn(String accountName) {
            Log.d(TAG, "JSBridge: backupSyncOptIn");
        }

        @JavascriptInterface
        public final void cancelFido2SignRequest() {
            Log.d(TAG, "JSBridge: cancelFido2SignRequest");
            fidoHandler.cancel();
        }

        @JavascriptInterface
        public void clearOldLoginAttempts() {
            Log.d(TAG, "JSBridge: clearOldLoginAttempts");
        }

        @JavascriptInterface
        public final void closeView() {
            Log.d(TAG, "JSBridge: closeView");
            closeWeb(false);
        }

        @JavascriptInterface
        public void fetchIIDToken(String entity) {
            Log.d(TAG, "JSBridge: fetchIIDToken");
        }

        @JavascriptInterface
        public final String fetchVerifiedPhoneNumber() {
            Log.d(TAG, "JSBridge: fetchVerifiedPhoneNumber");
            return null;
        }

        @SuppressWarnings("MissingPermission")
        @JavascriptInterface
        public final String getAccounts() {
            Log.d(TAG, "JSBridge: getAccounts");
            Account[] accountsByType = accountManager.getAccountsByType(accountType);
            JSONArray json = new JSONArray();
            for (Account account : accountsByType) {
                json.put(account.name);
            }
            return json.toString();
        }

        @JavascriptInterface
        public final String getAllowedDomains() {
            Log.d(TAG, "JSBridge: getAllowedDomains");
            return new JSONArray().toString();
        }

        @JavascriptInterface
        public final String getAndroidId() {
            long androidId = LastCheckinInfo.read(LoginActivity.this).getAndroidId();
            Log.d(TAG, "JSBridge: getAndroidId");
            if (androidId == 0 || androidId == -1) return null;
            return Long.toHexString(androidId);
        }

        @JavascriptInterface
        public final int getAuthModuleVersionCode() {
            return GMS_VERSION_CODE;
        }

        @JavascriptInterface
        public final int getBuildVersionSdk() {
            return Build.VERSION.SDK_INT;
        }

        @JavascriptInterface
        public int getDeviceContactsCount() {
            return -1;
        }

        @JavascriptInterface
        public final int getDeviceDataVersionInfo() {
            return 1;
        }

        @JavascriptInterface
        public final void getDroidGuardResult(String s) {
            Log.d(TAG, "JSBridge: getDroidGuardResult");
            try {
                JSONArray array = new JSONArray(s);
                StringBuilder sb = new StringBuilder();
                sb.append(getAndroidId()).append(":").append(getBuildVersionSdk()).append(":").append(getPlayServicesVersionCode());
                for (int i = 0; i < array.length(); i++) {
                    sb.append(":").append(array.getString(i));
                }
                String dg = Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(sb.toString().getBytes()), 0);
                dgHandler.start(dg);
            } catch (Exception e) {
                // Ignore
            }
        }

        @JavascriptInterface
        public final String getFactoryResetChallenges() {
            return new JSONArray().toString();
        }

        @JavascriptInterface
        public final String getPhoneNumber() {
            return null;
        }

        @JavascriptInterface
        public final int getPlayServicesVersionCode() {
            return GMS_VERSION_CODE;
        }

        @JavascriptInterface
        public final String getSimSerial() {
            return null;
        }

        @JavascriptInterface
        public final int getSimState() {
            return SIM_STATE_UNKNOWN;
        }

        @JavascriptInterface
        public final void goBack() {
            Log.d(TAG, "JSBridge: goBack");
        }

        @JavascriptInterface
        public final boolean hasPhoneNumber() {
            return false;
        }

        @JavascriptInterface
        public final boolean hasTelephony() {
            return false;
        }

        @JavascriptInterface
        public final void hideKeyboard() {
            inputMethodManager.hideSoftInputFromWindow(webView.getWindowToken(), 0);
        }

        @JavascriptInterface
        public final boolean isUserOwner() {
            return true;
        }

        @JavascriptInterface
        public final void launchEmergencyDialer() {
            Log.d(TAG, "JSBridge: launchEmergencyDialer");
        }

        @JavascriptInterface
        public final void log(String s) {
            Log.d(TAG, "JSBridge: log");
        }

        @JavascriptInterface
        public final void notifyOnTermsOfServiceAccepted() {
            Log.d(TAG, "JSBridge: notifyOnTermsOfServiceAccepted");
        }

        @JavascriptInterface
        public final void sendFido2SkUiEvent(String event) {
            Log.d(TAG, "JSBridge: sendFido2SkUiEvent");
            fidoHandler.onEvent(event);
        }

        @JavascriptInterface
        public final void setAccountIdentifier(String accountName) {
            Log.d(TAG, "JSBridge: setAccountIdentifier");
        }

        @JavascriptInterface
        public void setAllActionsEnabled(boolean z) {
            Log.d(TAG, "JSBridge: setAllActionsEnabled");
        }

        @TargetApi(HONEYCOMB)
        @JavascriptInterface
        public final void setBackButtonEnabled(boolean backButtonEnabled) {
            if (SDK_INT <= GINGERBREAD_MR1) return;
            int visibility = getWindow().getDecorView().getSystemUiVisibility();
            if (backButtonEnabled)
                visibility &= -STATUS_BAR_DISABLE_BACK;
            else
                visibility |= STATUS_BAR_DISABLE_BACK;
            getWindow().getDecorView().setSystemUiVisibility(visibility);
        }


        @JavascriptInterface
        public final void setNewAccountCreated() {
            Log.d(TAG, "JSBridge: setNewAccountCreated");
        }

        @JavascriptInterface
        public void setPrimaryActionEnabled(boolean z) {
            Log.d(TAG, "JSBridge: setPrimaryActionEnabled");
        }

        @JavascriptInterface
        public void setPrimaryActionLabel(String str, int i) {
            Log.d(TAG, "JSBridge: setPrimaryActionLabel: " + str);
        }

        @JavascriptInterface
        public void setSecondaryActionEnabled(boolean z) {
            Log.d(TAG, "JSBridge: setSecondaryActionEnabled");
        }

        @JavascriptInterface
        public void setSecondaryActionLabel(String str, int i) {
            Log.d(TAG, "JSBridge: setSecondaryActionLabel: " + str);
        }

        @JavascriptInterface
        public final void showKeyboard() {
            inputMethodManager.showSoftInput(webView, SHOW_IMPLICIT);
        }

        @JavascriptInterface
        public final void showView() {
            runOnUiThread(() -> webView.setVisibility(VISIBLE));
        }

        @JavascriptInterface
        public final void skipLogin() {
            Log.d(TAG, "JSBridge: skipLogin");
            finish();
        }

        @JavascriptInterface
        public final void startAfw() {
            Log.d(TAG, "JSBridge: startAfw");
        }

        @JavascriptInterface
        public final void startFido2SignRequest(String request) {
            Log.d(TAG, "JSBridge: startFido2SignRequest");
            fidoHandler.startSignRequest(request);
        }

    }
}
