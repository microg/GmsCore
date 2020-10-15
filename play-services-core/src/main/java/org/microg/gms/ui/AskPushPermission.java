package org.microg.gms.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.R;

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

        setContentView(R.layout.ask_gcm);

        try {
            PackageManager pm = getPackageManager();
            final ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            CharSequence label = pm.getApplicationLabel(info);

            ((TextView) findViewById(R.id.permission_message)).setText(Html.fromHtml("Allow <b>" + label + "</b> to register for push notifications?"));
            findViewById(R.id.permission_allow_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (answered) return;
                    database.noteAppKnown(packageName, true);
                    answered = true;
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(EXTRA_EXPLICIT, true);
                    resultReceiver.send(Activity.RESULT_OK, bundle);
                    finish();
                }
            });
            findViewById(R.id.permission_deny_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (answered) return;
                    database.noteAppKnown(packageName, false);
                    answered = true;
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(EXTRA_EXPLICIT, true);
                    resultReceiver.send(Activity.RESULT_CANCELED, bundle);
                    finish();
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            finish();
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
