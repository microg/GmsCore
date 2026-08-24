/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.os.IInterface;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import org.microg.gms.common.Hide;

@Hide
public abstract class BaseGmsClient<T extends IInterface> {
    public interface ConnectionProgressReportCallbacks {
        void onReportServiceBinding(@NonNull ConnectionResult connectionResult);
    }

    public interface SignOutCallbacks {
        void onSignOutComplete();
    }
}
