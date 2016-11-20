package org.microg.gms.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.R;

import org.microg.gms.gcm.GcmDatabase;
import org.microg.gms.gcm.PushRegisterService;

import static org.microg.gms.gcm.GcmConstants.EXTRA_APP;
import static org.microg.gms.gcm.GcmConstants.EXTRA_PENDING_INTENT;

public class AskPushPermission extends FragmentActivity {

    private GcmDatabase database;

    private String packageName;
    private Intent intent;
    private boolean answered;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new GcmDatabase(this);

        packageName = getIntent().getStringExtra(EXTRA_APP);
        intent = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT);

        if (database.getApp(packageName) != null) {
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
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            PushRegisterService.registerAndReply(AskPushPermission.this, intent, packageName);
                        }
                    }).start();
                    finish();
                }
            });
            findViewById(R.id.permission_deny_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (answered) return;
                    database.noteAppKnown(packageName, false);
                    answered = true;
                    PushRegisterService.replyNotAvailable(AskPushPermission.this, intent, packageName);
                    finish();
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!answered) {
            PushRegisterService.replyNotAvailable(AskPushPermission.this, intent, packageName);
            answered = true;
        }
        database.close();
    }
}
