package com.google.android.gms.common.safeparcel;

import android.os.Parcelable;

public interface SafeParcelable extends Parcelable {
	public static final String NULL = "SAFE_PARCELABLE_NULL_STRING";
	int SAFE_PARCEL_MAGIC = 20293;
}
