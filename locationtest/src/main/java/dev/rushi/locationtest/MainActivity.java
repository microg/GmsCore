package dev.rushi.locationtest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

/**
 * Dummy app that requests location from the fork through the STANDARD client
 * path (LocationServices.getFusedLocationProviderClient), which goes through
 * MultiConnectionKeeper to find the GMS package.
 */
public class MainActivity extends Activity {
    private static final String TAG = "LocationTest";
    private static final int REQ_PERMISSION = 1;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tv = new TextView(this);
        tv.setPadding(48, 48, 48, 48);
        tv.setTextSize(16);
        setContentView(tv);
        tv.setText("Requesting location via LocationServices...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSION);
        } else {
            requestLocation();
        }
    }

    private void show(final String text) {
        runOnUiThread(() -> tv.setText(text));
    }

    private void requestLocation() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        log("client=" + client.getClass().getName());

        Task<Location> last = client.getLastLocation();
        last.addOnCompleteListener(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                Location loc = t.getResult();
                log("last location: " + loc.getLatitude() + "," + loc.getLongitude() + " (" + loc.getProvider() + ")");
                show("LAST LOCATION via LocationServices:\n" + loc.getLatitude() + ", " + loc.getLongitude()
                        + " (provider=" + loc.getProvider() + ", acc=" + loc.getAccuracy() + "m)\n\nRequesting updates...");
            } else {
                Log.i(TAG, "getLastLocation failed: " + t.getException());
                show("getLastLocation failed: " + (t.getException() != null ? t.getException() : "null") + "\n\nRequesting updates...");
            }
        });

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build();
        client.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || result.getLastLocation() == null) {
                    log("update: null");
                    return;
                }
                Location loc = result.getLastLocation();
                log("UPDATE: " + loc.getLatitude() + "," + loc.getLongitude() + " (" + loc.getProvider() + ")");
                show("UPDATE via LocationServices:\n" + loc.getLatitude() + ", " + loc.getLongitude()
                        + " (provider=" + loc.getProvider() + ", acc=" + loc.getAccuracy() + "m)\n"
                        + "time=" + loc.getTime());
            }
        }, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        } else {
            tv.setText("Permission denied");
        }
    }

    private void log(String msg) {
        Log.i(TAG, msg);
    }
}
