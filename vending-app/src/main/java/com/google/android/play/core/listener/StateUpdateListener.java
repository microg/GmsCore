/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.listener;

/**
 * Base interface for state update listeners.
 */
public interface StateUpdateListener<StateT> {
    /**
     * Callback triggered whenever the state has changed.
     */
    void onStateUpdate(StateT state);
}
