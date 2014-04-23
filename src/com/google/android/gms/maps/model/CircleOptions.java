package com.google.android.gms.maps.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.safeparcel.SafeReader;
import com.google.android.gms.common.safeparcel.SafeWriter;

public class CircleOptions implements Parcelable {
	public static Creator<CircleOptions> CREATOR = new Creator<CircleOptions>() {
		public CircleOptions createFromParcel(Parcel source) {
			return new CircleOptions(source);
		}

		public CircleOptions[] newArray(int size) {
			return new CircleOptions[size];
		}
	};
	private int versionCode;
	private LatLng center;
	private double radius;
	private float strokeWidth;
	private int strokeColor;
	private int fillColor;
	private float zIndex;
	private boolean visisble;

	public CircleOptions() {
	}

	private CircleOptions(Parcel in) {
		int end = SafeReader.readStart(in);
		while (in.dataPosition() < end) {
			int position = SafeReader.readSingleInt(in);
			switch (SafeReader.halfOf(position)) {
				case 1:
					versionCode = SafeReader.readInt(in, position);
					break;
				case 2:
					center = SafeReader.readParcelable(in, position, LatLng.CREATOR);
					break;
				case 3:
					radius = SafeReader.readDouble(in, position);
					break;
				case 4:
					strokeWidth = SafeReader.readFloat(in, position);
					break;
				case 5:
					strokeColor = SafeReader.readInt(in, position);
					break;
				case 6:
					fillColor = SafeReader.readInt(in, position);
					break;
				case 7:
					zIndex = SafeReader.readFloat(in, position);
					break;
				case 8:
					visisble = SafeReader.readBool(in, position);
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
		SafeWriter.write(dest, 2, center, flags, false);
		SafeWriter.write(dest, 3, radius);
		SafeWriter.write(dest, 4, strokeWidth);
		SafeWriter.write(dest, 5, strokeColor);
		SafeWriter.write(dest, 6, fillColor);
		SafeWriter.write(dest, 7, zIndex);
		SafeWriter.write(dest, 8, visisble);
		SafeWriter.writeEnd(dest, start);
	}
}
