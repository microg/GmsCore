package com.google.android.gms.maps.model;

import android.os.Parcel;
import android.os.Parcelable;

public class MarkerOptions implements Parcelable {

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}

	public MarkerOptions() {
	}

	private MarkerOptions(Parcel in) {
	}

	public static Creator<MarkerOptions> CREATOR = new Creator<MarkerOptions>() {
		public MarkerOptions createFromParcel(Parcel source) {
			return new MarkerOptions(source);
		}

		public MarkerOptions[] newArray(int size) {
			return new MarkerOptions[size];
		}
	};
}
