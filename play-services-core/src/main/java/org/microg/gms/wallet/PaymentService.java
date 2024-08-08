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

package org.microg.gms.wallet;

import android.os.RemoteException;

import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.ConnectionInfo;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;

public class PaymentService extends BaseService {

    public static final Feature[] FEATURES = new Feature[]{
        new Feature("wallet", 1L),
        new Feature("wallet_biometric_auth_keys", 1L),
        new Feature("wallet_payment_dynamic_update", 2L),
        new Feature("wallet_1p_initialize_buyflow", 1L),
        new Feature("wallet_warm_up_ui_process", 1L),
        new Feature("wallet_get_setup_wizard_intent", 4L),
        new Feature("wallet_get_payment_card_recognition_intent", 1L),
        new Feature("wallet_save_instrument", 1L)
    };

    public PaymentService() {
        super("GmsWalletPaySvc", GmsService.WALLET);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.features = FEATURES;
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, new OwServiceImpl(this), connectionInfo);
    }
}
