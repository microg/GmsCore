/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.maps.vtm.markup;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.maps.model.internal.IMarkerDelegate;
import org.microg.gms.maps.vtm.GoogleMapImpl;
import org.microg.gms.maps.vtm.ResourcesContainer;
import org.microg.gms.maps.vtm.R;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;

public class InfoWindow {
    private static final String TAG = InfoWindow.class.getName();
    private Context context;
    private View window;
    private GoogleMapImpl map;
    private MarkerImpl marker;

    public InfoWindow(Context context, final GoogleMapImpl map, final MarkerImpl marker) {
        super();
        this.context = context;
        this.map = map;
        this.marker = marker;
    }

    public void setWindow(View view) {
        window = view;
        if (window != null) {
            window.measure(0, 0);
        }
    }

    public boolean isComplete() {
        return window != null;
    }

    public void setContent(View view) {
        if (view == null)
            return;
        setWindow(new DefaultWindow(view));
    }

    public void buildDefault() {
        if (marker.getTitle() != null)
            setContent(new DefaultContent());
    }

    public void destroy() {
        if (window instanceof DefaultWindow) {
            ((DefaultWindow) window).removeAllViews();
        }
    }

    public IMarkerDelegate getMarker() {
        return marker;
    }

    /*
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (window != null && marker.getHeight() != -1 && !shadow) {
            try {
                Log.d(TAG, "draw InfoWindow");
                window.measure(0, 0);
                window.layout(0, 0, window.getMeasuredWidth(), window.getMeasuredHeight());
                //Point point = mapView.getProjection().toPixels(marker.getPosition().toGeoPoint(), null);
                
                // osmdroid 4.1 bugfix
                Point zero = mapView.getProjection().toPixels(new GeoPoint(0, 0), null);
                point.offset(-zero.x, -zero.y);
                

                
                point.offset(-window.getMeasuredWidth() / 2, -window.getMeasuredHeight() - marker.getHeight());
                Log.d(TAG, point.toString());
                canvas.save();
                canvas.translate(point.x, point.y);
                window.draw(canvas);
                canvas.restore();
            } catch (Exception e) {
                // This is not remote...
            }
        }
    }

    @Override
    public boolean onTap(GeoPoint p, MapView mapView) {
        try {
            IOnInfoWindowClickListener listener = null; //map.getInfoWindowClickListener();
            if (listener != null) {
                Point clickPoint = mapView.getProjection().toPixels(p, null);
                Point markerPoint = mapView.getProjection().toPixels(marker.getPosition().toGeoPoint(), null);
                Rect rect = new Rect(markerPoint.x - (window.getMeasuredWidth() / 2),
                        markerPoint.y - marker.getHeight() - window.getMeasuredHeight(),
                        markerPoint.x + (window.getMeasuredWidth() / 2),
                        markerPoint.y - marker.getHeight());
                if (rect.contains(clickPoint.x, clickPoint.y)) {
                    try {
                        listener.onInfoWindowClick(marker);
                    } catch (RemoteException e) {
                        Log.w(TAG, e);
                    }
                    return true;
                }
                
            }
        } catch (Exception e) {
            // This is not remote...
        }
        return false;
    }
*/

    private class DefaultWindow extends FrameLayout {
        @SuppressWarnings("deprecation")
        public DefaultWindow(View view) {
            super(context);
            addView(view);
            if (SDK_INT > ICE_CREAM_SANDWICH_MR1) {
                setBackground(ResourcesContainer.get().getDrawable(R.drawable.maps_default_window));
            } else {
                setBackgroundDrawable(ResourcesContainer.get().getDrawable(R.drawable.maps_default_window));
            }
        }
    }

    private class DefaultContent extends LinearLayout {
        public DefaultContent() {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            TextView title = new TextView(context);
            title.setTextAppearance(context,
                    android.R.style.TextAppearance_DeviceDefault_Medium_Inverse);
            title.setText(marker.getTitle());
            addView(title);
            if (marker.getSnippet() != null) {
                TextView snippet = new TextView(context);
                snippet.setTextAppearance(context,
                        android.R.style.TextAppearance_DeviceDefault_Inverse);
                snippet.setText(marker.getSnippet());
                addView(snippet);
            }
        }
    }
}
