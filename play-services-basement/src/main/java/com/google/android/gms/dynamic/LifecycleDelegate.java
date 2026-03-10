/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamic;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface LifecycleDelegate {
    View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onDestroyView();

    void onInflate(@NonNull Activity activity, @NonNull Bundle options, @Nullable Bundle onInflate);

    void onLowMemory();

    void onPause();

    void onResume();

    void onSaveInstanceState(@NonNull Bundle outState);

    void onStart();

    void onStop();
}
