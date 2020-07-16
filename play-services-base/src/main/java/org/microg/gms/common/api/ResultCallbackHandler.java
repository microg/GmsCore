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

package org.microg.gms.common.api;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

class ResultCallbackHandler<R extends Result> extends Handler {
    private static final String TAG = "GmsResultCbackHandler";
    public static final int CALLBACK_ON_COMPLETE = 1;
    public static final int CALLBACK_ON_TIMEOUT = 2;

    public ResultCallbackHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case CALLBACK_ON_COMPLETE:
                OnCompleteObject<R> o = (OnCompleteObject<R>) msg.obj;
                Log.d(TAG, "handleMessage() : onResult(" + o.result + ")");
                o.callback.onResult(o.result);
                break;
            case CALLBACK_ON_TIMEOUT:
                // TODO
                break;
        }
    }

    public void sendResultCallback(ResultCallback<R> callback, R result) {
        Message message = new Message();
        message.what = CALLBACK_ON_COMPLETE;
        message.obj = new OnCompleteObject<R>(callback, result);
        sendMessage(message);
    }

    public void sendTimeoutResultCallback(AbstractPendingResult pendingResult, long millis) {

    }

    public static class OnCompleteObject<R extends Result> {
        public ResultCallback<R> callback;
        public R result;

        public OnCompleteObject(ResultCallback<R> callback, R result) {
            this.callback = callback;
            this.result = result;
        }
    }
}
