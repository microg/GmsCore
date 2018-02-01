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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

import java.io.IOException;
import java.util.Locale;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.GINGERBREAD_MR1;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.common.Constants.MAX_REFERENCE_VERSION;

public class LoginActivity extends AssistantActivity {
    public static final String TMPL_NEW_ACCOUNT = "new_account";
    public static final String EXTRA_TMPL = "tmpl";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_TOKEN = "masterToken";
    public static final int STATUS_BAR_DISABLE_BACK = 0x00400000;

    private static final String TAG = "GmsAuthLoginBrowser";
    private static final String EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup";
    private static final String PROGRAMMATIC_AUTH_URL = "https://accounts.google.com/o/oauth2/programmatic_auth";
    private static final String MAGIC_USER_AGENT = " MinuteMaid";
    private static final String COOKIE_OAUTH_TOKEN = "oauth_token";

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
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "pageFinished: " + url);
                if ("identifier".equals(Uri.parse(url).getFragment()))
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.setVisibility(VISIBLE);
                        }
                    });
                if ("close".equals(Uri.parse(url).getFragment()))
                    closeWeb(false);
                if (url.startsWith(PROGRAMMATIC_AUTH_URL))
                    closeWeb(true);
            }
        });
        if (getIntent().hasExtra(EXTRA_TOKEN)) {
            if (getIntent().hasExtra(EXTRA_EMAIL)) {
                AccountManager accountManager = AccountManager.get(LoginActivity.this);
                Account account = new Account(getIntent().getStringExtra(EXTRA_EMAIL), accountType);
                accountManager.addAccountExplicitly(account, getIntent().getStringExtra(EXTRA_TOKEN), null);
                retrieveGmsToken(account);
            } else {
                retrieveRtToken(getIntent().getStringExtra(EXTRA_TOKEN));
            }
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
            CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    start();
                }
            });
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
        prepareWebViewSettings(webView.getSettings());
        return webView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void prepareWebViewSettings(WebSettings settings) {
        settings.setUserAgentString(settings.getUserAgentString() + MAGIC_USER_AGENT);
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
            if (LastCheckinInfo.read(this).androidId == 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Runnable next;
                        next = checkin(false) ? new Runnable() {
                            @Override
                            public void run() {
                                loadLoginPage();
                            }
                        } : new Runnable() {
                            @Override
                            public void run() {
                                showError(R.string.auth_general_error_desc);
                            }
                        };
                        LoginActivity.this.runOnUiThread(next);
                    }
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

    private void setMessage(@StringRes  int res) {
        setMessage(getText(res));
    }

    private void setMessage(CharSequence text) {
        ((TextView) findViewById(R.id.description_text)).setText(text);
    }

    private void loadLoginPage() {
        String tmpl = getIntent().hasExtra(EXTRA_TMPL) ? getIntent().getStringExtra(EXTRA_TMPL) : TMPL_NEW_ACCOUNT;
        webView.loadUrl(buildUrl(tmpl, Utils.getLocale(this)));
    }

    private void closeWeb(boolean programmaticAuth) {
        setMessage(R.string.auth_finalize);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.setVisibility(INVISIBLE);
            }
        });
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
                .service("ac2dm")
                .token(oAuthToken).isAccessToken()
                .addAccount()
                .getAccountId()
                .systemPartition()
                .hasPermission()
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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showError(R.string.auth_general_error_desc);
                                    setNextButtonText(android.R.string.ok);
                                }
                            });
                            state = -2;
                        }
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException: " + exception);
                    }
                });
    }

    private void retrieveGmsToken(final Account account) {
        final AuthManager authManager = new AuthManager(this, account.name, GMS_PACKAGE_NAME, "ac2dm");
        authManager.setPermitted(true);
        new AuthRequest().fromContext(this)
                .appIsGms()
                .service(authManager.getService())
                .email(account.name)
                .token(AccountManager.get(this).getPassword(account))
                .systemPartition()
                .hasPermission()
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
                        Log.w(TAG, "onException: " + exception);
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
        @SuppressWarnings("MissingPermission")
        @JavascriptInterface
        public final String getAccounts() {
            Account[] accountsByType = accountManager.getAccountsByType(accountType);
            JSONArray json = new JSONArray();
            for (Account account : accountsByType) {
                json.put(account.name);
            }
            return json.toString();
        }

        @JavascriptInterface
        public final String getAllowedDomains() {
            return new JSONArray().toString();
        }

        @JavascriptInterface
        public final String getAndroidId() {
            long androidId = LastCheckinInfo.read(LoginActivity.this).androidId;
            if (androidId == 0 || androidId == -1) return null;
            return Long.toHexString(androidId);
        }

        @JavascriptInterface
        public final int getBuildVersionSdk() {
            return SDK_INT;
        }

        @JavascriptInterface
        public final void getDroidGuardResult(String s) {
            Log.d(TAG, "JSBridge: getDroidGuardResult: " + s);
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
            return MAX_REFERENCE_VERSION;
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
            Log.d(TAG, "JSBridge: hideKeyboard");
            inputMethodManager.hideSoftInputFromWindow(webView.getWindowToken(), 0);
        }

        @JavascriptInterface
        public final void launchEmergencyDialer() {
            Log.d(TAG, "JSBridge: launchEmergencyDialer");
        }

        @JavascriptInterface
        public final void notifyOnTermsOfServiceAccepted() {
            Log.d(TAG, "Terms of service accepted. (who cares?)");
        }

        @TargetApi(HONEYCOMB)
        @JavascriptInterface
        public final void setBackButtonEnabled(boolean backButtonEnabled) {
            Log.d(TAG, "JSBridge: setBackButtonEnabled: " + backButtonEnabled);
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
            Log.d(TAG, "New account created. (who cares?)");
        }

        @JavascriptInterface
        public final void showKeyboard() {
            Log.d(TAG, "JSBridge: showKeyboard");
            inputMethodManager.showSoftInput(webView, SHOW_IMPLICIT);
        }

        @JavascriptInterface
        public final void showView() {
            Log.d(TAG, "JSBridge: showView");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.setVisibility(VISIBLE);
                }
            });
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

    }
}
