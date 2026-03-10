package com.google.android.gms.people.internal;

import android.os.Bundle;

import com.google.android.gms.people.internal.IPeopleCallbacks;
import com.google.android.gms.common.server.FavaDiagnosticsEntity;
import com.google.android.gms.common.internal.ICancelToken;

interface IPeopleService {
    // void loadOwners1(IPeopleCallbacks var1, boolean var2, boolean var3, String var4, String var5);
    // void loadCirclesOld(IPeopleCallbacks var1, String var2, String var3, String var4, int var5, String var6);
    // void loadPeopleOld(IPeopleCallbacks var1, String var2, String var3, String var4, in List<String> var5, int var6, boolean var7, long var8);
    // void loadAvatarLegacy(IPeopleCallbacks var1, String var2, int var3, int var4);
    // void loadContactImageLegacy(IPeopleCallbacks var1, long var2, boolean var4);
    // void blockPerson(IPeopleCallbacks var1, String var2, String var3, String var4, boolean var5);
    // Bundle syncRawContact(in Uri var1);
    // void loadPeopleForAggregation8(IPeopleCallbacks var1, String var2, String var3, String var4, boolean var5, int var6);
    // void setSyncToContactsSettings(IPeopleCallbacks var1, String var2, boolean var3, in String[] var4);

    // Bundle startSync(String var1, String var2);
    // void requestSync(IPeopleCallbacks var1, String var2, String var3, in Uri var4);
    // void updatePersonCirclesOld(IPeopleCallbacks var1, String var2, String var3, String var4, in List<String> var5, in List<String> var6);
    // boolean isSyncToContactsEnabled();
    // Bundle requestSyncOld(String var1, String var2);
    // void setAvatar(IPeopleCallbacks var1, String var2, String var3, in Uri var4, boolean var5);
    // void loadCircles(IPeopleCallbacks var1, String var2, String var3, String var4, int var5, String var6, boolean var7);
    // Bundle requestSyncOld19(String var1, String var2, long var3);
    // void loadPeople20(IPeopleCallbacks var1, String var2, String var3, String var4, in List<String> var5, int var6, boolean var7, long var8, String var10, int var11);
    // void loadPeopleLive(IPeopleCallbacks var1, String var2, String var3, String var4, int var5, String var6);
    // void updatePersonCircles(IPeopleCallbacks var1, String var2, String var3, String var4, in List<String> var5, in List<String> var6, in FavaDiagnosticsEntity var7);
    // void loadRemoteImageLegacy(IPeopleCallbacks var1, String var2);
    // void loadContactsGaiaIds24(IPeopleCallbacks var1, String var2, String var3);
    // Bundle requestSyncOld25(String var1, String var2, long var3, boolean var5);
    // void addCircle(IPeopleCallbacks var1, String var2, String var3, String var4, String var5);
    // void addPeopleToCircle(IPeopleCallbacks var1, String var2, String var3, String var4, in List<String> var5);

    Bundle registerDataChangedListener(IPeopleCallbacks callbacks, boolean register, String var3, String var4, int scopes) = 10;
    void loadCircles(IPeopleCallbacks callbacks, String account, String pageGaiaId, String circleId, int type, String var6, boolean var7) = 18;
    Bundle requestSync(String account, String var2, long var3, boolean var5, boolean var6) = 204;
    void loadOwners(IPeopleCallbacks callbacks, boolean var2, boolean var3, String account, String var5, int sortOrder) = 304;
    void loadPeopleForAggregation(IPeopleCallbacks callbacks, String account, String var3, String filter, int var5, boolean var6, int var7, int var8, String var9, boolean var10, int var11, int var12) = 401;
    ICancelToken loadOwnerAvatar(IPeopleCallbacks callbacks, String account, String pageId, int size, int flags) = 504;
    ICancelToken loadAutocompleteList(IPeopleCallbacks callbacks, String account, String pageId, boolean directorySearch, String var5, String query, int autocompleteType, int var8, int numberOfResults, boolean var10) = 506;
}
