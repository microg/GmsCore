/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay;

import android.content.Context;
import android.os.IBinder;
import com.google.android.gms.pay.internal.IThirdPartyPayService;
import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;

public class ThirdPartyPayApiClient extends GmsClient<IThirdPartyPayService> {
    public ThirdPartyPayApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.PAY.SECONDARY_ACTIONS[0]);
        serviceId = GmsService.PAY.SERVICE_ID;
    }

    @Override
    protected IThirdPartyPayService interfaceFromBinder(IBinder binder) {
        return IThirdPartyPayService.Stub.asInterface(binder);
    }
}
