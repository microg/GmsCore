package org.microg.gms.common.api;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

class CallbackHandler<R extends Result> extends Handler {
    public static final int CALLBACK_ON_COMPLETE = 1;
    public static final int CALLBACK_ON_TIMEOUT = 2;
    
    public CallbackHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case CALLBACK_ON_COMPLETE:
                OnCompleteObject<R> o = (OnCompleteObject<R>) msg.obj;
                o.callback.onResult(o.result);
                break;
            case CALLBACK_ON_TIMEOUT:
                // TODO
                break;
        }
    }

    public void sendResultCallback(ResultCallback<R> callback, R result) {
        
    }

    public void sendTimeoutResultCallback(AbstractPendingResult pendingResult, long millis) {
        
    }

    public static class OnCompleteObject<R extends Result> {
        public ResultCallback<R> callback;
        public R result;
    }
}
