package com.google.android.gms.maps.internal;

import android.content.res.Resources;

public class ResourcesContainer {
	private static Resources resources;
	public static void set(Resources resources) {
		ResourcesContainer.resources = resources;
	}
	public static Resources get() {
		if (resources == null) {
			throw new IllegalStateException("Resources have not been initialized");
		} else {
			return resources;
		}
	}
}
