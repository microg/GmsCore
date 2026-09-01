/*
 * Copyright (C) 2013-2019 microG Project Team
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

package org.microg.gms.ui;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuItemCompat;

import com.google.android.gms.R;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import com.google.android.gms.location.places.internal.PlaceImpl;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.microg.gms.location.LocationConstants;
//import org.microg.gms.maps.vtm.BackendMapView;
//import org.microg.gms.maps.vtm.GmsMapsTypeHelper;
//import org.oscim.core.MapPosition;
//import org.oscim.event.Event;
//import org.oscim.map.Map;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static org.microg.gms.location.LocationConstants.EXTRA_PRIMARY_COLOR;
import static org.microg.gms.location.LocationConstants.EXTRA_PRIMARY_COLOR_DARK;
//import static org.microg.gms.maps.vtm.GmsMapsTypeHelper.fromLatLngBounds;

public class


PlacePickerActivity extends AppCompatActivity /*implements Map.UpdateListener*/ {
    private static final String TAG = "GmsPlacePicker";

    private PlaceImpl place;
//    private BackendMapView mapView;
    private Intent resultIntent;
    private AtomicBoolean geocoderInProgress = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resultIntent = new Intent();
        place = new PlaceImpl();

        setContentView(R.layout.pick_place);

        Toolbar toolbar = (Toolbar) findViewById(org.microg.tools.ui.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (getIntent().hasExtra(EXTRA_PRIMARY_COLOR)) {
            toolbar.setBackgroundColor(getIntent().getIntExtra(EXTRA_PRIMARY_COLOR, 0));
            if (SDK_INT >= 21)
                getWindow().setStatusBarColor(getIntent().getIntExtra(EXTRA_PRIMARY_COLOR_DARK, 0));
            ((TextView) findViewById(R.id.place_picker_title)).setTextColor(getIntent().getIntExtra(EXTRA_PRIMARY_COLOR_DARK, 0));
        }

//        mapView = (BackendMapView) findViewById(R.id.map);
//        mapView.map().getEventLayer().enableRotation(false);
//        mapView.map().getEventLayer().enableTilt(false);
//        mapView.map().events.bind(this);

        LatLngBounds latLngBounds = getIntent().getParcelableExtra(LocationConstants.EXTRA_BOUNDS);
        if (latLngBounds != null) {
            place.viewport = latLngBounds;
//            MapPosition mp = new MapPosition();
//            mp.setByBoundingBox(fromLatLngBounds(latLngBounds), mapView.map().getWidth(), mapView.map().getHeight());
//            mapView.map().getMapPosition(mp);
        } else {
            if (ActivityCompat.checkSelfPermission(PlacePickerActivity.this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PlacePickerActivity.this, new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, 0);
            } else {
                updateMapFromLocationManager();
            }
        }

        findViewById(R.id.place_picker_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultIntent.putExtra(LocationConstants.EXTRA_STATUS, SafeParcelableSerializer.serializeToBytes(new Status(CommonStatusCodes.SUCCESS)));
                resultIntent.putExtra(LocationConstants.EXTRA_PLACE, SafeParcelableSerializer.serializeToBytes(place));
                resultIntent.putExtra(LocationConstants.EXTRA_FINAL_BOUNDS, SafeParcelableSerializer.serializeToBytes(place.viewport));
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void updateMapFromLocationManager() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location last = null;
        for (String provider : lm.getAllProviders()) {
            if (lm.isProviderEnabled(provider)) {
                Location t = lm.getLastKnownLocation(provider);
                if (t != null && (last == null || t.getTime() > last.getTime())) {
                    last = t;
                }
            }
        }
        Log.d(TAG, "Set location to " + last);
        if (last != null) {
//            mapView.map().setMapPosition(new MapPosition(last.getLatitude(), last.getLongitude(), 4096));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            for (int grantResult : grantResults) {
                if (grantResult != PERMISSION_GRANTED) return;
            }
            updateMapFromLocationManager();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pick_place, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_action_search));
        // TODO: search
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mapView.onResume();
    }

    @Override
    protected void onPause() {
//        mapView.onPause();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
    @Override
    public void onMapEvent(Event event, MapPosition position) {
//        place.viewport = GmsMapsTypeHelper.toLatLngBounds(mapView.map().viewport().getBBox(null, 0));
//        resultIntent.putExtra(LocationConstants.EXTRA_FINAL_BOUNDS, place.viewport);
//        place.latLng = GmsMapsTypeHelper.toLatLng(position.getGeoPoint());
        place.name = "";
        place.address = "";
        updateInfoText();
        if (geocoderInProgress.compareAndSet(false, true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        LatLng ll = null;
                        while (ll != place.latLng) {
                            ll = place.latLng;
                            Thread.sleep(1000);
                        }
                        Geocoder geocoder = new Geocoder(PlacePickerActivity.this);
                        List<Address> addresses = geocoder.getFromLocation(place.latLng.latitude, place.latLng.longitude, 1);
                        if (addresses != null && !addresses.isEmpty() && addresses.get(0).getMaxAddressLineIndex() > 0) {
                            Address address = addresses.get(0);
                            StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                            for (int i = 1; i < address.getMaxAddressLineIndex(); ++i) {
                                if (i == 1 && sb.toString().equals(address.getFeatureName())) {
                                    sb = new StringBuilder(address.getAddressLine(i));
                                    continue;
                                }
                                sb.append(", ").append(address.getAddressLine(i));
                            }
                            if (place.latLng == ll) {
                                place.address = sb.toString();
                                place.name = address.getFeatureName();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateInfoText();
                                    }
                                });
                            }
                        }
                    } catch (Exception ignored) {
                        Log.w(TAG, ignored);
                    } finally {
                        geocoderInProgress.lazySet(false);
                    }
                }
            }).start();
        }
    }*/

    private void updateInfoText() {
        if (TextUtils.isEmpty(place.address)) {
            ((TextView) findViewById(R.id.place_picker_info)).setText(getString(R.string.place_picker_location_lat_lng, place.latLng.latitude, place.latLng.longitude));
        } else if (TextUtils.isEmpty(place.name)) {
            ((TextView) findViewById(R.id.place_picker_info)).setText(place.address);
        } else {
            ((TextView) findViewById(R.id.place_picker_info)).setText(Html.fromHtml("<b>" + place.name + "</b>, " + place.address));
        }
    }
}
