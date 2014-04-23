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

package com.google.android.gms.maps;

import android.os.Parcel;
import com.google.android.gms.common.safeparcel.SafeParcelable;
import com.google.android.gms.common.safeparcel.SafeReader;
import com.google.android.gms.common.safeparcel.SafeWriter;
import com.google.android.gms.maps.model.CameraPosition;

public class GoogleMapOptions implements SafeParcelable {
	private int versionCode;
	private int zOrderOnTop;
	private boolean useViewLifecycleInFragment;
	private int mapType;
	private CameraPosition camera;
	private boolean zoomControlsEnabled;
	private boolean compassEnabled;
	private boolean scrollGesturesEnabled;
	private boolean zoomGesturesEnabled;
	private boolean tiltGesturesEnabled;
	private boolean rotateGesturesEnabled;


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		int start = SafeWriter.writeStart(dest);
		SafeWriter.write(dest, 1, versionCode);
		SafeWriter.write(dest, 2, zOrderOnTop);
		SafeWriter.write(dest, 3, useViewLifecycleInFragment);
		SafeWriter.write(dest, 4, mapType);
		SafeWriter.write(dest, 5, camera, flags, false);
		SafeWriter.write(dest, 6, zoomControlsEnabled);
		SafeWriter.write(dest, 7, compassEnabled);
		SafeWriter.write(dest, 8, scrollGesturesEnabled);
		SafeWriter.write(dest, 9, zoomGesturesEnabled);
		SafeWriter.write(dest, 10, tiltGesturesEnabled);
		SafeWriter.write(dest, 11, rotateGesturesEnabled);
		SafeWriter.writeEnd(dest, start);
	}

	public GoogleMapOptions() {
	}

	private GoogleMapOptions(Parcel in) {
		int end = SafeReader.readStart(in);
		while (in.dataPosition() < end) {
			int position = SafeReader.readSingleInt(in);
			switch (SafeReader.halfOf(position)) {
				case 1:
					versionCode = SafeReader.readInt(in, position);
					break;
				case 2:
					zOrderOnTop = SafeReader.readInt(in, position);
					break;
				case 3:
					useViewLifecycleInFragment = SafeReader.readBool(in, position);
					break;
				case 4:
					mapType = SafeReader.readInt(in, position);
					break;
				case 5:
					camera = SafeReader.readParcelable(in, position, CameraPosition.CREATOR);
					break;
				case 6:
					zoomControlsEnabled = SafeReader.readBool(in, position);
					break;
				case 7:
					compassEnabled = SafeReader.readBool(in, position);
					break;
				case 8:
					scrollGesturesEnabled = SafeReader.readBool(in, position);
					break;
				case 9:
					zoomGesturesEnabled = SafeReader.readBool(in, position);
					break;
				case 10:
					tiltGesturesEnabled = SafeReader.readBool(in, position);
					break;
				case 11:
					rotateGesturesEnabled = SafeReader.readBool(in, position);
					break;
				default:
					SafeReader.skip(in, position);
			}
		}
		if (in.dataPosition() > end) {
			throw new SafeReader.ReadException("Overread allowed size end=" + end, in);
		}
	}

	public static Creator<GoogleMapOptions> CREATOR = new Creator<GoogleMapOptions>() {
		public GoogleMapOptions createFromParcel(Parcel source) {
			return new GoogleMapOptions(source);
		}

		public GoogleMapOptions[] newArray(int size) {
			return new GoogleMapOptions[size];
		}
	};
}
