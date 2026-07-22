package com.google.android.gms.appinvite.internal;


import com.google.android.gms.appinvite.internal.IAppInviteCallbacks;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.common.api.Status;


interface IAppInviteService {
    void updateInvitationOnInstall(IAppInviteCallbacks callback, String invitationId) = 0;
    void convertInvitation(IAppInviteCallbacks callback, String invitationId) = 1;
    void getInvitation(IAppInviteCallbacks callback) = 2;
}
