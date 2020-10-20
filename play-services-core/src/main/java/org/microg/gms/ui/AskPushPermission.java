package org.microg.gms.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.Html;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.mgoogle.android.gms.R;

import org.microg.gms.gcm.GcmDatabase;
import org.microg.gms.gcm.PushRegisterService;

import static org.microg.gms.gcm.GcmConstants.EXTRA_APP;
import static org.microg.gms.gcm.GcmConstants.EXTRA_KID;
import static org.microg.gms.gcm.GcmConstants.EXTRA_PENDING_INTENT;

public class AskPushPermission extends FragmentActivity {
    public static final String EXTRA_REQUESTED_PACKAGE = "package";
    public static final String EXTRA_RESULT_RECEIVER = "receiver";
    public static final String EXTRA_EXPLICIT = "explicit";

    private GcmDatabase database;

    private String packageName;
    private ResultReceiver resultReceiver;
    private boolean answered;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new GcmDatabase(this);

        packageName = getIntent().getStringExtra(EXTRA_REQUESTED_PACKAGE);
        resultReceiver = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (packageName == null || resultReceiver == null) {
            answered = true;
            finish();
            return;
        }

        if (database.getApp(packageName) != null) {
            resultReceiver.send(Activity.RESULT_OK, Bundle.EMPTY);
            answered = true;
            finish();
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!answered) {
            resultReceiver.send(Activity.RESULT_CANCELED, Bundle.EMPTY);
        }
        database.close();
    }
}
