/*
 * Copyright 2013-2015 µg Project Team
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
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.google.android.gms.R;

import org.microg.gms.auth.AuthClient;
import org.microg.gms.auth.AuthManager;
import org.microg.gms.auth.AuthRequest;
import org.microg.gms.auth.AuthResponse;
import org.microg.gms.common.Constants;
import org.microg.gms.common.Utils;

import java.util.Locale;

public class LoginActivity extends AssistantActivity {
    public static final String TMPL_NEW_ACCOUNT = "new_account";
    public static final String EXTRA_TMPL = "tmpl";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_TOKEN = "masterToken";

    private static final String TAG = "GmsAuthLoginBrowser";
    private static final String EMBEDDED_SETUP_URL = "https://accounts.google.com/EmbeddedSetup";
    private static final String MAGIC_USER_AGENT = " MinuteMaid";
    private static final String COOKIE_OAUTH_TOKEN = "oauth_token";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = createWebView(this);
        webView.addJavascriptInterface(new JsBridge(), "mm");
        ((ViewGroup) findViewById(R.id.auth_root)).addView(webView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if ("close".equals(Uri.parse(url).getFragment()))
                    closeWeb();
            }
        });
        if (getIntent().hasExtra(EXTRA_TOKEN)) {
            if (getIntent().hasExtra(EXTRA_EMAIL)) {
                AccountManager accountManager = AccountManager.get(LoginActivity.this);
                Account account = new Account(getIntent().getStringExtra(EXTRA_EMAIL), "com.google");
                accountManager.addAccountExplicitly(account, getIntent().getStringExtra(EXTRA_TOKEN), null);
                retrieveGmsToken(account);
            } else {
                retrieveRtToken(getIntent().getStringExtra(EXTRA_TOKEN));
            }
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        load();
                    }
                });
            } else {
                //noinspection deprecation
                CookieManager.getInstance().removeAllCookie();
                load();
            }
        }
    }

    private static WebView createWebView(Context context) {
        WebView webView = new WebView(context);
        webView.setVisibility(View.INVISIBLE);
        webView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        prepareWebViewSettings(webView.getSettings());
        return webView;
    }

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

    private void load() {
        String tmpl = getIntent().hasExtra(EXTRA_TMPL) ? getIntent().getStringExtra(EXTRA_TMPL) : TMPL_NEW_ACCOUNT;
        webView.loadUrl(buildUrl(tmpl, Utils.getLocale(this)));
    }

    private void closeWeb() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.setVisibility(View.INVISIBLE);
            }
        });
        String cookies = CookieManager.getInstance().getCookie(EMBEDDED_SETUP_URL);
        String[] temp = cookies.split(";");
        for (String ar1 : temp) {
            if (ar1.trim().startsWith(COOKIE_OAUTH_TOKEN + "=")) {
                String[] temp1 = ar1.split("=");
                retrieveRtToken(temp1[1]);
            }
        }
        // TODO: Error message
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
                .getResponseAsync(new AuthClient.GmsAuthCallback() {
                    @Override
                    public void onResponse(AuthResponse response) {
                        AccountManager accountManager = AccountManager.get(LoginActivity.this);
                        Account account = new Account(response.email, "com.google");
                        if (accountManager.addAccountExplicitly(account, response.token, null)) {
                            accountManager.setAuthToken(account, "SID", response.Sid);
                            accountManager.setAuthToken(account, "LSID", response.LSid);
                            accountManager.setUserData(account, "flags", "1");
                            accountManager.setUserData(account, "services", response.services);
                            accountManager.setUserData(account, "oauthAccessToken", "1");
                            accountManager.setUserData(account, "firstName", response.firstName);
                            accountManager.setUserData(account, "lastName", response.lastName);

                            retrieveGmsToken(account);
                            setResult(RESULT_OK);
                        } else {
                            // TODO: Error message
                            Log.w(TAG, "Account NOT created!");
                            setResult(RESULT_CANCELED);
                        }
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException: " + exception);
                    }
                });
    }

    private void retrieveGmsToken(final Account account) {
        final String service = "ac2dm";
        new AuthRequest().fromContext(this)
                .appIsGms()
                .service(service)
                .email(account.name)
                .token(AccountManager.get(this).getPassword(account))
                .systemPartition()
                .hasPermission()
                .addAccount()
                .getAccountId()
                .getResponseAsync(new AuthClient.GmsAuthCallback() {
                    @Override
                    public void onResponse(AuthResponse response) {
                        AuthManager.storeResponse(LoginActivity.this, account,
                                Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1,
                                service, response);
                        retrieveGmsKeyUserinfoProfile(account);
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException: " + exception);
                    }
                });
    }

    private void retrieveGmsKeyUserinfoProfile(final Account account) {
        final String service = "oauth2:https://www.googleapis.com/auth/userinfo.profile";
        new AuthRequest().fromContext(this)
                .appIsGms().callerIsGms()
                .service(service)
                .email(account.name)
                .token(AccountManager.get(this).getPassword(account))
                .systemPartition()
                .hasPermission()
                .getAccountId()
                .getResponseAsync(new AuthClient.GmsAuthCallback() {
                    @Override
                    public void onResponse(AuthResponse response) {
                        AuthManager.storeResponse(LoginActivity.this, account,
                                Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1,
                                service, response);
                        finish();
                    }

                    @Override
                    public void onException(Exception exception) {
                        Log.w(TAG, "onException: " + exception);
                    }
                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack() && webView.getVisibility() == View.VISIBLE) {
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
                .appendQueryParameter("cc", locale.getCountry().toLowerCase())
                .appendQueryParameter("langCountry", locale.toString().toLowerCase())
                .appendQueryParameter("hl", locale.toString().replace("_", "-"))
                .appendQueryParameter("tmpl", tmpl)
                .build().toString();
    }

    private class JsBridge {
        @JavascriptInterface
        public void showView() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}
