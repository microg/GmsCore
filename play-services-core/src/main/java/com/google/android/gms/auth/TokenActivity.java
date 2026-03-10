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

package com.google.android.gms.auth;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class TokenActivity extends Activity {
    private static final String TAG = "GmsAuthTokenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        extras.get("KEY");
        Log.d(TAG, extras.toString());
        /*AccountManager accountManager = AccountManager.get(this);
        accountManager.getAuthToken(new Account(extras.getString("authAccount"), "com.google"), extras.getString("service"), extras.getBundle("callerExtras"), this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle result = future.getResult();
                    if (result != null) {
                        result.get("KEY");
                        Log.d("TokenActivity", result.toString());
                    } else {
                        Log.d("TokenActivity", "null-result");
                    }
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
            }
        }, new Handler(getMainLooper()));*/
    }
}
