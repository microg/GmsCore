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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.microg.gms.auth.AuthManagerServiceImpl;

public class GetToken extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new AuthManagerServiceImpl(this);
    }
}
