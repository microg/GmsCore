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

package com.google.android.gms.cast.framework.internal;

import android.util.Log;

import com.google.android.gms.cast.framework.ISession;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

public class SessionImpl extends ISession.Stub {
    private static final String TAG = SessionImpl.class.getSimpleName();

    @Override
    public void notifySessionEnded(int error) {
        Log.d(TAG, "unimplemented Method: notifySessionEnded");
    }

    @Override
    public boolean isConnected() {
        Log.d(TAG, "unimplemented Method: isConnected");
        return true;
    }

    @Override
    public boolean isResuming() {
        Log.d(TAG, "unimplemented Method: isResuming");
        return false;
    }

    @Override
    public IObjectWrapper getWrappedThis() {
        return ObjectWrapper.wrap(this);
    }
}
