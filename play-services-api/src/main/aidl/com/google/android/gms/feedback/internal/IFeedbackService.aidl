/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.feedback.internal;

import com.google.android.gms.feedback.FeedbackOptions;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import com.google.android.gms.googlehelp.GoogleHelp;
import com.google.android.gms.feedback.ErrorReport;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;


interface IFeedbackService {

    boolean startFeedbackFlow(in ErrorReport errorReport) = 0;

    boolean silentSendFeedback(in ErrorReport errorReport) = 2;

    void saveFeedbackDataAsync(in Bundle bundle, long id) = 3;

    void saveFeedbackDataAsyncWithOption(in FeedbackOptions options, in Bundle bundle, long id) = 4;

    void startFeedbackFlowAsync(in ErrorReport errorReport, long id) = 5;

    boolean isValidConfiguration(in FeedbackOptions options) = 6;

}