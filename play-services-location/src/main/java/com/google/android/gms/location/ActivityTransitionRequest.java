/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import org.microg.safeparcel.AutoSafeParcelable;

/**
 * The request object for apps to get notified when user's activity changes.
 */
public class ActivityTransitionRequest extends AutoSafeParcelable {
    public static final Creator<ActivityTransitionRequest> CREATOR = new AutoCreator<>(ActivityTransitionRequest.class);
}
