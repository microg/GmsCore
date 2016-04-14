/*
 * Copyright 2013-2016 microG Project Team
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

package com.google.android.gms.gcm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * <code>WakefulBroadcastReceiver</code> that receives GCM messages and delivers them to an
 * application-specific {@link com.google.android.gms.gcm.GcmListenerService} subclass.
 * <p/>
 * This receiver should be declared in your application's manifest file as follows:
 * <p/>
 * <pre>
 * <receiver
 *     android:name="com.google.android.gms.gcm.GcmReceiver"
 *     android:exported="true"
 *     android:permission="com.google.android.c2dm.permission.SEND" >
 *     <intent-filter>
 *         <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *         <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
 *         <category android:name="YOUR_PACKAGE_NAME" />
 *     </intent-filter>
 * </receiver></pre>
 * The <code>com.google.android.c2dm.permission.SEND</code> permission is held by Google Play
 * services. This prevents other apps from invoking the broadcast receiver.
 */
public class GcmReceiver extends WakefulBroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        throw new UnsupportedOperationException();
    }

}