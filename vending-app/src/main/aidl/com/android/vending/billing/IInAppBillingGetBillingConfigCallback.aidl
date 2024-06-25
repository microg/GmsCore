// IInAppBillingGetBillingConfigCallback.aidl
package com.android.vending.billing;

import android.os.Bundle;

// Declare any non-default types here with import statements

interface IInAppBillingGetBillingConfigCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void callback(in Bundle bundle);
}