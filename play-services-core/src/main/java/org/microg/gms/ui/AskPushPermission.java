package org.microg.gms.ui;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.R;

import org.microg.gms.gcm.GcmDatabase;

public class AskPushPermission extends FragmentActivity {
    public static final String EXTRA_REQUESTED_PACKAGE = "package";
    public static final String EXTRA_RESULT_RECEIVER = "receiver";
    public static final String EXTRA_FORCE_ASK = "force";
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
        boolean force = getIntent().getBooleanExtra(EXTRA_FORCE_ASK, false);
        if (packageName == null || (resultReceiver == null && !force)) {
            answered = true;
            finish();
            return;
        }

        if (!force && database.getApp(packageName) != null) {
            resultReceiver.send(Activity.RESULT_OK, Bundle.EMPTY);
            answered = true;
            finish();
            return;
        }

        setContentView(R.layout.ask_gcm);

        try {
            PackageManager pm = getPackageManager();
            final ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            String label = pm.getApplicationLabel(info).toString();
            String raw = getString(R.string.gcm_allow_app_popup, label);
            SpannableString s = new SpannableString(raw);
            s.setSpan(new StyleSpan(Typeface.BOLD), raw.indexOf(label), raw.indexOf(label) + label.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            ((TextView) findViewById(R.id.permission_message)).setText(s);
            findViewById(R.id.permission_allow_button).setOnClickListener(v -> {
                if (answered) return;
                database.noteAppKnown(packageName, true);
                answered = true;
                Bundle bundle = new Bundle();
                bundle.putBoolean(EXTRA_EXPLICIT, true);
                if (resultReceiver != null) resultReceiver.send(Activity.RESULT_OK, bundle);
                finish();
            });
            findViewById(R.id.permission_deny_button).setOnClickListener(v -> {
                if (answered) return;
                database.noteAppKnown(packageName, false);
                answered = true;
                Bundle bundle = new Bundle();
                bundle.putBoolean(EXTRA_EXPLICIT, true);
                if (resultReceiver != null) resultReceiver.send(Activity.RESULT_CANCELED, bundle);
                finish();
            });
        } catch (PackageManager.NameNotFoundException e) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!answered) {
            if (resultReceiver != null) resultReceiver.send(Activity.RESULT_CANCELED, Bundle.EMPTY);
        }
        database.close();
    }
}
