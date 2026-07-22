/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing;

import static org.microg.vending.billing.ui.PlayWebViewActivityKt.KEY_WEB_VIEW_ACCOUNT;
import static org.microg.vending.billing.ui.PlayWebViewActivityKt.KEY_WEB_VIEW_ACTION;
import static org.microg.vending.billing.ui.PlayWebViewActivityKt.KEY_WEB_VIEW_OPEN_URL;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.vending.R;

import org.microg.gms.auth.AuthConstants;
import org.microg.vending.billing.ui.PlayWebViewActivity;
import org.microg.vending.billing.ui.WebViewAction;

public class PurchaseActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String authAccount = getIntent().getStringExtra("authAccount");
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE);
        String referralUrl = getIntent().getStringExtra("referral_url");
        if (!TextUtils.isEmpty(referralUrl) && accounts.length > 0) {
            Account currAccount = null;
            for (Account account : accounts) {
                if (account.name.equals(authAccount)) {
                    currAccount = account;
                    break;
                }
            }
            if (!referralUrl.startsWith("https")) {
                referralUrl = referralUrl.replace("http", "https");
            }
            //Perform host judgment on URLs to prevent risky URLs from being exploited
            if (referralUrl.startsWith("https://play.google.com/store")) {
                Intent intent = new Intent(this, PlayWebViewActivity.class);
                intent.putExtra(KEY_WEB_VIEW_ACTION, WebViewAction.OPEN_GP_PRODUCT_DETAIL.toString());
                intent.putExtra(KEY_WEB_VIEW_OPEN_URL, referralUrl);
                intent.putExtra(KEY_WEB_VIEW_ACCOUNT, currAccount);
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.pay_disabled), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.pay_disabled), Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
