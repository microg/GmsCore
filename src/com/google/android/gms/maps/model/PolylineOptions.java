package com.google.android.gms.maps.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class PolylineOptions implements Parcelable {
	private int versionCode;
	private List<LatLng> points;
	private float width;
	private int color;
	private float zIndex;
	private boolean visible;
	private boolean geodesic;


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO
	}

	public PolylineOptions() {
	}

	private PolylineOptions(Parcel in) {
		// TODO
	}

	public static Creator<PolylineOptions> CREATOR = new Creator<PolylineOptions>() {
		public PolylineOptions createFromParcel(Parcel source) {
			return new PolylineOptions(source);
		}

		public PolylineOptions[] newArray(int size) {
			return new PolylineOptions[size];
		}
	};
}
