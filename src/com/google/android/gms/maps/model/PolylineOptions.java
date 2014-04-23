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
