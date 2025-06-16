package org.microg.gms.wearos;

import android.app.Activity; import android.content.pm.PackageManager; import android.os.Bundle; import android.Manifest; import android.view.Gravity; import android.view.View; import android.view.animation.AlphaAnimation; import android.view.animation.Animation; import android.widget.Button; import android.widget.LinearLayout; import android.widget.Toast; import androidx.core.app.ActivityCompat; import androidx.core.content.ContextCompat; import android.content.res.Configuration; import android.graphics.Color;

public class MainActivity extends Activity {

private static final int PERMISSION_REQUEST_CODE = 1001;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setGravity(Gravity.CENTER);
    layout.setPadding(20, 20, 20, 20);

    if (isDarkMode) {
        layout.setBackgroundColor(Color.BLACK);
    } else {
        layout.setBackgroundColor(Color.WHITE);
    }

    Button pairNowButton = new Button(this);
    pairNowButton.setText("Pair Now");
    pairNowButton.setOnClickListener(v -> {
        animateButton(v);
        Toast.makeText(this, "Pairing started (placeholder)", Toast.LENGTH_SHORT).show();
    });

    Button checkSyncButton = new Button(this);
    checkSyncButton.setText("Check Sync");
    checkSyncButton.setOnClickListener(v -> {
        animateButton(v);
        Toast.makeText(this, "Sync status: OK (placeholder)", Toast.LENGTH_SHORT).show();
    });

    layout.addView(pairNowButton);
    layout.addView(checkSyncButton);
    setContentView(layout);

    checkAndRequestPermissions();
}

private void animateButton(View view) {
    Animation fade = new AlphaAnimation(0.3f, 1.0f);
    fade.setDuration(300);
    view.startAnimation(fade);
}

private void checkAndRequestPermissions() {
    String[] permissions = {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT
    };

    boolean allGranted = true;
    for (String perm : permissions) {
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            allGranted = false;
            break;
        }
    }

    if (!allGranted) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSION_REQUEST_CODE) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Some permissions denied. Features may not work.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
    }
}

}

