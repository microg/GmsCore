package com.google.android.gms.maps.model;

import android.os.Parcel;
import com.google.android.gms.common.safeparcel.SafeParcelable;
import com.google.android.gms.common.safeparcel.SafeReader;
import com.google.android.gms.common.safeparcel.SafeWriter;

public class LatLngBounds implements SafeParcelable {
	public static Creator<LatLngBounds> CREATOR = new Creator<LatLngBounds>() {
		public LatLngBounds createFromParcel(Parcel source) {
			return new LatLngBounds(source);
		}

		public LatLngBounds[] newArray(int size) {
			return new LatLngBounds[size];
		}
	};
	private int versionCode;
	private LatLng southWest;
	private LatLng northEast;

	public LatLngBounds() {
	}

	private LatLngBounds(Parcel in) {
		int end = SafeReader.readStart(in);
		while (in.dataPosition() < end) {
			int position = SafeReader.readSingleInt(in);
			switch (SafeReader.halfOf(position)) {
				case 1:
					versionCode = SafeReader.readInt(in, position);
					break;
				case 2:
					southWest = SafeReader.readParcelable(in, position, LatLng.CREATOR);
					break;
				case 3:
					northEast = SafeReader.readParcelable(in, position, LatLng.CREATOR);
					break;
				default:
					SafeReader.skip(in, position);
			}
		}
		if (in.dataPosition() > end) {
			throw new SafeReader.ReadException("Overread allowed size end=" + end, in);
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		int start = SafeWriter.writeStart(dest);
		SafeWriter.write(dest, 1, versionCode);
		SafeWriter.write(dest, 2, southWest, flags, false);
		SafeWriter.write(dest, 3, northEast, flags, false);
		SafeWriter.writeEnd(dest, start);
	}
}
