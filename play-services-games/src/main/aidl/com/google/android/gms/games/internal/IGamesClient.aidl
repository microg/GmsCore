package com.google.android.gms.games.internal;

import com.google.android.gms.games.internal.popup.PopupLocationInfoParcelable;

interface IGamesClient {
    PopupLocationInfoParcelable getPopupLocationInfoParcelable() = 1000;
}
