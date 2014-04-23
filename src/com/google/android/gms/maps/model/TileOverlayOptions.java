package com.google.android.gms.maps.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TileOverlayOptions implements Parcelable {

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}

	public TileOverlayOptions() {
	}

	private TileOverlayOptions(Parcel in) {
	}

	public static Creator<TileOverlayOptions> CREATOR = new Creator<TileOverlayOptions>() {
		public TileOverlayOptions createFromParcel(Parcel source) {
			return new TileOverlayOptions(source);
		}

		public TileOverlayOptions[] newArray(int size) {
			return new TileOverlayOptions[size];
		}
	};
}
