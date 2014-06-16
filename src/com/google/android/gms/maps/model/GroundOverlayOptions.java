/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.safeparcel.SafeReader;
import com.google.android.gms.common.safeparcel.SafeWriter;
import com.google.android.gms.dynamic.IObjectWrapper;
import org.microg.gms.maps.bitmap.BitmapDescriptor;

public class GroundOverlayOptions implements Parcelable {
	public static Creator<GroundOverlayOptions> CREATOR = new Creator<GroundOverlayOptions>() {
		public GroundOverlayOptions createFromParcel(Parcel source) {
			return new GroundOverlayOptions(source);
		}

		public GroundOverlayOptions[] newArray(int size) {
			return new GroundOverlayOptions[size];
		}
	};
	private int versionCode;
	private BitmapDescriptor wrappedImage;
	private LatLng location;
	private float width;
	private float height;
	private LatLngBounds bounds;
	private float bearing;
	private float zIndex;
	private boolean visible;
	private float transparency;
	private float anchorU;
	private float anchorV;

	public GroundOverlayOptions() {
	}

	private GroundOverlayOptions(Parcel in) {
		int end = SafeReader.readStart(in);
		while (in.dataPosition() < end) {
			int position = SafeReader.readSingleInt(in);
			switch (SafeReader.halfOf(position)) {
				case 1:
					versionCode = SafeReader.readInt(in, position);
					break;
				case 2:
					wrappedImage = new BitmapDescriptor(IObjectWrapper.Stub.asInterface(SafeReader.readBinder(in, position)));
					break;
				case 3:
					location = SafeReader.readParcelable(in, position, LatLng.CREATOR);
					break;
				case 4:
					width = SafeReader.readFloat(in, position);
					break;
				case 5:
					height = SafeReader.readFloat(in, position);
					break;
				case 6:
					bounds = SafeReader.readParcelable(in, position, LatLngBounds.CREATOR);
					break;
				case 7:
					bearing = SafeReader.readFloat(in, position);
					break;
				case 8:
					zIndex = SafeReader.readFloat(in, position);
					break;
				case 9:
					visible = SafeReader.readBool(in, position);
					break;
				case 10:
					transparency = SafeReader.readFloat(in, position);
					break;
				case 11:
					anchorU = SafeReader.readFloat(in, position);
					break;
				case 12:
					anchorV = SafeReader.readFloat(in, position);
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
		SafeWriter.write(dest, 2, wrappedImage.getRemoteObject().asBinder(), false);
		SafeWriter.write(dest, 3, location, flags, false);
		SafeWriter.write(dest, 4, width);
		SafeWriter.write(dest, 5, height);
		SafeWriter.write(dest, 6, bounds, flags, false);
		SafeWriter.write(dest, 7, bearing);
		SafeWriter.write(dest, 8, zIndex);
		SafeWriter.write(dest, 9, visible);
		SafeWriter.write(dest, 10, transparency);
		SafeWriter.write(dest, 11, anchorU);
		SafeWriter.write(dest, 12, anchorV);
		SafeWriter.writeEnd(dest, start);
	}
}
