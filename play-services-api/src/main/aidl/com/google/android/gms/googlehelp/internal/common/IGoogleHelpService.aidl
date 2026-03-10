package com.google.android.gms.googlehelp.internal.common;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.gms.feedback.FeedbackOptions;
import com.google.android.gms.googlehelp.GoogleHelp;
import com.google.android.gms.googlehelp.InProductHelp;
import com.google.android.gms.googlehelp.SupportRequestHelp;
import com.google.android.gms.googlehelp.internal.common.IGoogleHelpCallbacks;

interface IGoogleHelpService {
    void processGoogleHelpAndPip(in GoogleHelp googleHelp, IGoogleHelpCallbacks callbacks) = 0;
    void processGoogleHelpAndPipWithBitmap(in GoogleHelp googleHelp, in Bitmap bitmap, IGoogleHelpCallbacks callbacks) = 1;
    oneway void saveAsyncHelpPsd(in Bundle bundle, long timestamp, in GoogleHelp googleHelp, IGoogleHelpCallbacks callbacks) = 7;
    oneway void saveAsyncFeedbackPsd(in Bundle bundle, long timestamp, in GoogleHelp googleHelp, IGoogleHelpCallbacks callbacks) = 8;
    oneway void saveAsyncFeedbackPsbd(in FeedbackOptions options, in Bundle bundle, long timestamp, in GoogleHelp googleHelp, IGoogleHelpCallbacks callbacks) = 9;
    oneway void requestChatSupport(in GoogleHelp googleHelp, String phoneNumber, String s2, IGoogleHelpCallbacks callbacks) = 10;
    oneway void requestC2cSupport(in GoogleHelp googleHelp, String phoneNumber, String s2, IGoogleHelpCallbacks callbacks) = 11;
    oneway void getSuggestions(in GoogleHelp googleHelp, IGoogleHelpCallbacks callbacks) = 12;
    oneway void getEscalationOptions(in GoogleHelp googleHelp, IGoogleHelpCallbacks callbacks) = 13;
    oneway void requestChatSupportWithSupportRequest(in SupportRequestHelp supportRequestHelp, IGoogleHelpCallbacks callbacks) = 14;
    oneway void requestC2cSupportWithSupportRequest(in SupportRequestHelp supportRequestHelp, IGoogleHelpCallbacks callbacks) = 15;
    void processInProductHelpAndPip(in InProductHelp inProductHelp, in Bitmap bitmap, IGoogleHelpCallbacks callbacks) = 16;
    oneway void getRealtimeSupportStatus(in GoogleHelp googleHelp, IGoogleHelpCallbacks callbacks) = 17;
}
