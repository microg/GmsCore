package org.microg.gms.ui;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import org.microg.gms.gcm.GcmDatabase;

public class AskPushPermission extends AppCompatActivity {
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
        boolean forceAsk = getIntent().getBooleanExtra(EXTRA_FORCE_ASK, false);
        if (packageName == null || (resultReceiver == null && !forceAsk)) {
            answered = true;
            finish();
            return;
        }

        if (!forceAsk && database.getApp(packageName) != null) {
            resultReceiver.send(Activity.RESULT_OK, Bundle.EMPTY);
            answered = true;
            finish();
            return;
        }

        try {
            View dialogView = getLayoutInflater().inflate(R.layout.ask_push_notification, null);
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

            MaterialButton allowButton = dialogView.findViewById(R.id.button_allow);
            MaterialButton denyButton = dialogView.findViewById(R.id.button_deny);

            String appLabel = pm.getApplicationLabel(appInfo).toString();
            ShapeableImageView iconView = dialogView.findViewById(R.id.application_icon);
            iconView.setImageDrawable(pm.getApplicationIcon(appInfo));

            String rawMessage = getString(R.string.gcm_allow_app_popup, appLabel);
            SpannableString spannableMessage = new SpannableString(rawMessage);
            int start = rawMessage.indexOf(appLabel);
            if (start >= 0) {
                spannableMessage.setSpan(new StyleSpan(Typeface.BOLD), start, start + appLabel.length(), SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
            }
            ((android.widget.TextView) dialogView.findViewById(R.id.permission_message)).setText(spannableMessage);

            allowButton.setText(getString(R.string.allow));
            denyButton.setText(getString(R.string.deny));

            AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(true).setOnCancelListener(d -> {
                if (answered) return;
                answered = true;
                sendResult(Activity.RESULT_CANCELED, false);
                finish();
            }).create();

            allowButton.setOnClickListener(v -> {
                if (answered) return;
                answered = true;
                database.noteAppKnown(packageName, true);
                sendResult(Activity.RESULT_OK, true);
                dialog.dismiss();
                finish();
            });

            denyButton.setOnClickListener(v -> {
                if (answered) return;
                answered = true;
                database.noteAppKnown(packageName, false);
                sendResult(Activity.RESULT_CANCELED, true);
                dialog.dismiss();
                finish();
            });

            dialog.show();
        } catch (PackageManager.NameNotFoundException e) {
            finish();
        }
    }

    private void sendResult(int resultCode, boolean explicit) {
        if (resultReceiver == null) return;
        Bundle bundle = new Bundle();
        if (explicit) bundle.putBoolean(EXTRA_EXPLICIT, true);
        resultReceiver.send(resultCode, bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!answered) {
            sendResult(Activity.RESULT_CANCELED, false);
        }
        if (database != null) {
            database.close();
        }
    }
}
