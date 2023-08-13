/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamic;

public interface OnDelegateCreatedListener<T extends LifecycleDelegate> {
    void onDelegateCreated(T delegate);
}
