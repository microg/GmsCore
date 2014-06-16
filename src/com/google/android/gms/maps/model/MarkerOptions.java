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

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.common.safeparcel.SafeReader;
import com.google.android.gms.dynamic.ObjectWrapper;

public class MarkerOptions implements Parcelable {

	private int versionCode = 1;
	private LatLng position;
	private String title;
	private String snippet;
	private BitmapDescriptor icon;
	private float anchorU = 0.5F;
	private float anchorV = 1F;
	private boolean draggable;
	private boolean visible;
	private boolean flat;
	private float rotation = 0F;
	private float infoWindowAnchorU = 0F;
	private float infoWindowAnchorV = 1F;
	private float alpha = 1F;

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
		int end = SafeReader.readStart(in);
		while (in.dataPosition() < end) {
			int position = SafeReader.readSingleInt(in);
			switch (SafeReader.halfOf(position)) {
				case 1:
					this.versionCode = SafeReader.readInt(in, position);
					break;
				case 2:
					this.position = SafeReader.readParcelable(in, position, LatLng.CREATOR);
					break;
				case 3:
					this.title = SafeReader.readString(in, position);
					break;
				case 4:
					this.snippet = SafeReader.readString(in, position);
					break;
				case 5:
					IBinder icon = SafeReader.readBinder(in, position);
					this.icon = icon == null ? null : new BitmapDescriptor(ObjectWrapper.asInterface(icon));
					break;
				case 6:
					this.anchorU = SafeReader.readFloat(in, position);
					break;
				case 7:
					this.anchorV = SafeReader.readFloat(in, position);
					break;
				case 8:
					this.draggable = SafeReader.readBool(in, position);
					break;
				case 9:
					this.visible = SafeReader.readBool(in, position);
					break;
				case 10:
					this.flat = SafeReader.readBool(in, position);
					break;
                case 11:
                    this.rotation = SafeReader.readFloat(in, position);
                    break;
                case 12:
                    this.infoWindowAnchorU = SafeReader.readFloat(in, position);
                    break;
                case 13:
                    this.infoWindowAnchorV = SafeReader.readFloat(in, position);
                    break;
                case 14:
                    this.rotation = SafeReader.readFloat(in, position);
                    break;
				default:
					SafeReader.skip(in, position);
					break;
			}
		}
		if (in.dataPosition() > end) {
			throw new SafeReader.ReadException("Overread allowed size end=" + end, in);
		}
	}

	public LatLng getPosition() {
		return position;
	}

	public String getTitle() {
		return title;
	}

	public String getSnippet() {
		return snippet;
	}

	public BitmapDescriptor getIcon() {
		return icon;
	}

	public float getAnchorU() {
		return anchorU;
	}

	public float getAnchorV() {
		return anchorV;
	}

	public boolean isDraggable() {
		return draggable;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isFlat() {
		return flat;
	}

	public float getRotation() {
		return rotation;
	}

	public float getInfoWindowAnchorU() {
		return infoWindowAnchorU;
	}

	public float getInfoWindowAnchorV() {
		return infoWindowAnchorV;
	}

	public float getAlpha() {
		return alpha;
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
