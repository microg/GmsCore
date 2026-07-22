/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay;

import android.content.Context;
import android.os.IBinder;
import org.microg.gms.common.GmsClient;
import com.google.android.gms.pay.internal.IPayService;
import org.microg.gms.common.GmsService;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;

public class PayApiClient extends GmsClient<IPayService> {
    public PayApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.PAY.ACTION);
        serviceId = GmsService.PAY.SERVICE_ID;
    }

    @Override
    protected IPayService interfaceFromBinder(IBinder binder) {
        return IPayService.Stub.asInterface(binder);
    }
}
