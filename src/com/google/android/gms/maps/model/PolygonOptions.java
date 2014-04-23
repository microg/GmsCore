package com.google.android.gms.maps.model;

import android.os.Parcel;
import android.os.Parcelable;

public class PolygonOptions implements Parcelable {

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}

	public PolygonOptions() {
	}

	private PolygonOptions(Parcel in) {
	}

	public static Creator<PolygonOptions> CREATOR = new Creator<PolygonOptions>() {
		public PolygonOptions createFromParcel(Parcel source) {
			return new PolygonOptions(source);
		}

		public PolygonOptions[] newArray(int size) {
			return new PolygonOptions[size];
		}
	};
}
