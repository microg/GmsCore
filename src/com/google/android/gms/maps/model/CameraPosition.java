package com.google.android.gms.maps.model;

import android.os.Parcel;
import com.google.android.gms.common.safeparcel.SafeParcelable;
import com.google.android.gms.common.safeparcel.SafeReader;
import com.google.android.gms.common.safeparcel.SafeWriter;

import java.util.Arrays;

public class CameraPosition implements SafeParcelable {
	public static Creator<CameraPosition> CREATOR = new Creator<CameraPosition>() {
		public CameraPosition createFromParcel(Parcel source) {
			return new CameraPosition(source);
		}

		public CameraPosition[] newArray(int size) {
			return new CameraPosition[size];
		}
	};
	private int versionCode;
	public LatLng target;
	public float zoom;
	public float tilt;
	public float bearing;

	private CameraPosition(Parcel in) {
		int end = SafeReader.readStart(in);
		while (in.dataPosition() < end) {
			int position = SafeReader.readSingleInt(in);
			switch (SafeReader.halfOf(position)) {
				case 1:
					versionCode = SafeReader.readInt(in, position);
					break;
				case 2:
					target = SafeReader.readParcelable(in, position, LatLng.CREATOR);
					break;
				case 3:
					zoom = SafeReader.readFloat(in, position);
					break;
				case 4:
					tilt = SafeReader.readFloat(in, position);
					break;
				case 5:
					bearing = SafeReader.readFloat(in, position);
					break;
				default:
					SafeReader.skip(in, position);
			}
		}
		if (in.dataPosition() > end) {
			throw new SafeReader.ReadException("Overread allowed size end=" + end, in);
		}
	}

	public CameraPosition(int versionCode, LatLng target, float zoom, float tilt, float bearing) {
		this.versionCode = versionCode;
		if (target == null) {
			throw new NullPointerException("null camera target");
		}
		this.target = target;
		this.zoom = zoom;
		if (tilt < 0 || 90 < tilt) {
			throw new IllegalArgumentException("Tilt needs to be between 0 and 90 inclusive");
		}
		this.tilt = tilt;
		if (bearing <= 0) {
			bearing += 360;
		}
		this.bearing = bearing % 360;
	}

	public CameraPosition(LatLng target, float zoom, float tilt, float bearing) {
		this(1, target, zoom, tilt, bearing);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[]{target, zoom, tilt, bearing});
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		int i = SafeWriter.writeStart(dest);
        SafeWriter.write(dest, 1, versionCode);
        SafeWriter.write(dest, 2, target, flags, false);
        SafeWriter.write(dest, 3, zoom);
        SafeWriter.write(dest, 4, tilt);
        SafeWriter.write(dest, 5, bearing);
        SafeWriter.writeEnd(dest, i);
	}

	public static CameraPosition create(LatLng latLng) {
		return new CameraPosition(latLng, 0, 0, 0);
	}
}
