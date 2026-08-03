package com.google.android.gms.rcs.constellation;

interface IConstellationCallback {
    void onResult(int statusCode, in Bundle data);
}
