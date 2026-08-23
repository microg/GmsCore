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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import org.microg.gms.gcm.GcmDatabase;

public class AskPushPermissionActivity extends AppCompatActivity {

    public static final String EXTRA_REQUESTED_PACKAGE = "package";
    public static final String EXTRA_RESULT_RECEIVER = "receiver";
    public static final String EXTRA_EXPLICIT = "explicit";

    private GcmDatabase database;
    private String packageName;
    private ResultReceiver resultReceiver;
    private boolean answered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new GcmDatabase(this);
        packageName = getIntent().getStringExtra(EXTRA_REQUESTED_PACKAGE);
        resultReceiver = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);

        if (packageName == null || database.getApp(packageName) != null) {
            if (packageName != null) sendResult(Activity.RESULT_OK, true);
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

            configureDialogViews(dialogView, pm, appInfo, allowButton, denyButton);

            AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(true).setOnCancelListener(d -> {
                if (answered) return;
                sendResult(Activity.RESULT_CANCELED, false);
                answered = true;
                finish();
            }).create();

            setupButtonListeners(allowButton, denyButton, dialog);

            dialog.show();

        } catch (PackageManager.NameNotFoundException e) {
            finish();
        }
    }

    private void configureDialogViews(View view, PackageManager pm, ApplicationInfo info, MaterialButton allowButton, MaterialButton denyButton) {
        String appLabel = pm.getApplicationLabel(info).toString();
        ShapeableImageView iconView = view.findViewById(R.id.application_icon);
        iconView.setImageDrawable(pm.getApplicationIcon(info));

        TextView messageView = view.findViewById(R.id.permission_message);
        String rawMessage = getString(R.string.gcm_allow_app_popup, appLabel);
        SpannableString spannableMessage = new SpannableString(rawMessage);
        int start = rawMessage.indexOf(appLabel);
        if (start >= 0) {
            int end = start + appLabel.length();
            spannableMessage.setSpan(new StyleSpan(Typeface.BOLD), start, end, SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
        }
        messageView.setText(spannableMessage);

        allowButton.setText(getString(R.string.allow));
        denyButton.setText(getString(R.string.deny));
    }

    private void setupButtonListeners(MaterialButton allowButton, MaterialButton denyButton, AlertDialog dialog) {
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
    }

    private void sendResult(int resultCode, boolean explicit) {
        if (resultReceiver != null) {
            Bundle bundle = new Bundle();
            if (explicit) bundle.putBoolean(EXTRA_EXPLICIT, true);
            resultReceiver.send(resultCode, bundle);
        }
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