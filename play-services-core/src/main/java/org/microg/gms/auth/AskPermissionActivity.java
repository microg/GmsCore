package org.microg.gms.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.microg.gms.common.PackageUtils;
import org.microg.gms.people.PeopleManager;

import java.io.IOException;
import java.util.Objects;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_ANDROID_PACKAGE_NAME;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_CALLER_PID;
import static android.accounts.AccountManager.KEY_CALLER_UID;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/** @noinspection deprecation*/
public class AskPermissionActivity extends AccountAuthenticatorActivity {
    public static final String EXTRA_FROM_ACCOUNT_MANAGER = "from_account_manager";
    public static final String EXTRA_CONSENT_DATA = "consent_data";

    private static final String TAG = "GmsAuthAskPermission";
    private AuthManager authManager;
    private IntentData data;

    private static class IntentData {
        private String accountName;
        private String accountType;
        private Account account;
        private String packageName;
        private String service;
        private int callerUid;
        private int callerPid;
        private ConsentData consentData;
        private boolean fromAccountManager = false;
        private CharSequence appLabel;
        private Drawable appIcon;

        private IntentData(Intent intent) {
            if (intent != null) {
                accountName = intent.getStringExtra(KEY_ACCOUNT_NAME);
                accountType = intent.getStringExtra(KEY_ACCOUNT_TYPE);
                packageName = intent.getStringExtra(KEY_ANDROID_PACKAGE_NAME);
                service = intent.getStringExtra(KEY_AUTHTOKEN);
                callerUid = intent.getIntExtra(KEY_CALLER_UID, 0);
                callerPid = intent.getIntExtra(KEY_CALLER_PID, 0);
                fromAccountManager = intent.hasExtra(EXTRA_FROM_ACCOUNT_MANAGER);
                if (intent.hasExtra(EXTRA_CONSENT_DATA)) {
                    try {
                        consentData = ConsentData.ADAPTER.decode(Objects.requireNonNull(intent.getByteArrayExtra(EXTRA_CONSENT_DATA)));
                    } catch (Exception e) {
                        Log.w(TAG, "ConsentData decode failed", e);
                    }
                }
            }
            if (accountName != null && accountType != null) {
                account = new Account(accountName, accountType);
            }
        }

        private void verify(Context context) throws Exception {
            if (accountName == null || accountType == null || account == null) throw new IllegalArgumentException("Required account information missing");
            if (packageName == null || service == null) throw new IllegalArgumentException("Required request information missing");
            if (callerUid == 0) throw new IllegalArgumentException("Required caller information missing");
            PackageUtils.getAndCheckPackage(context, packageName, callerUid, callerPid);

            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            appLabel = packageManager.getApplicationLabel(applicationInfo);
            appIcon = packageManager.getApplicationIcon(applicationInfo);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        data = new IntentData(getIntent());
        try {
            data.verify(this);
        } catch (Exception e) {
            Log.w(TAG, "Verification failed", e);
            finish();
            return;
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(data.packageName.hashCode());

        authManager = new AuthManager(this, data.accountName, data.packageName, data.service);

        showPermissionDialog();
    }

    private void showPermissionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.ask_permission, null);

        ShapeableImageView accountPhoto = dialogView.findViewById(R.id.account_photo);
        ShapeableImageView appIcon = dialogView.findViewById(R.id.app_icon);
        TextView title = dialogView.findViewById(R.id.permission_title);
        RecyclerView permissionsList = dialogView.findViewById(R.id.permissions_list);
        LinearProgressIndicator progressBar = dialogView.findViewById(R.id.progress_bar);
        MaterialButton allowButton = dialogView.findViewById(R.id.button_allow);
        MaterialButton denyButton = dialogView.findViewById(R.id.button_deny);

        appIcon.setImageDrawable(data.appIcon);
        title.setText(isOAuth() ? getString(R.string.ask_scope_permission_title, data.appLabel) :
                getString(R.string.ask_service_permission_title, data.appLabel));

        permissionsList.setAdapter(new PermissionAdapter());

        loadAccountPhoto(accountPhoto);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        denyButton.setOnClickListener(v -> onDeny(dialog));
        allowButton.setOnClickListener(v -> onAllow(dialog, progressBar, allowButton, denyButton));

        dialog.show();
    }

    private void loadAccountPhoto(ImageView target) {
        Bitmap profileIcon = PeopleManager.getOwnerAvatarBitmap(this, data.accountName, false);
        if (profileIcon != null) {
            target.setImageBitmap(profileIcon);
        } else {
            new Thread(() -> {
                final Bitmap freshProfileIcon = PeopleManager.getOwnerAvatarBitmap(AskPermissionActivity.this, data.accountName, true);
                runOnUiThread(() -> target.setImageBitmap(freshProfileIcon));
            }).start();
        }
    }

    public void onAllow(AlertDialog dialog, LinearProgressIndicator progressBar, MaterialButton allowButton, MaterialButton denyButton) {
        authManager.setPermitted(true);
        allowButton.setEnabled(false);
        denyButton.setEnabled(false);
        progressBar.setVisibility(VISIBLE);

        new Thread(() -> {
            Bundle result = null;
            try {
                AuthResponse response = authManager.requestAuthWithBackgroundResolution(data.fromAccountManager);
                result = new Bundle();
                result.putString(KEY_AUTHTOKEN, response.auth);
                result.putString(KEY_ACCOUNT_NAME, data.accountName);
                result.putString(KEY_ACCOUNT_TYPE, data.accountType);
                result.putString(KEY_ANDROID_PACKAGE_NAME, data.packageName);
                result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
            } catch (IOException e) {
                Log.w(TAG, e);
            }
            final Bundle finalResult = result;
            runOnUiThread(() -> {
                Intent resultIntent = new Intent();
                if (finalResult != null) {
                    setAccountAuthenticatorResult(finalResult);
                    resultIntent.putExtras(finalResult);
                }
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }).start();
    }

    public void onDeny(AlertDialog dialog) {
        authManager.setPermitted(false);
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void finish() {
        if (data != null && data.packageName != null) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(data.packageName.hashCode());
        }
        super.finish();
    }

    private boolean isOAuth() {
        return data.service.startsWith("oauth2:") || data.service.startsWith("oauth:");
    }

    private String getScopeLabel(String scope) {
        ConsentData.ScopeDetails matched = findScopeDetails(scope);
        if (matched != null && matched.title != null) {
            return matched.title;
        }
        String labelResourceId = "permission_scope_";
        String escapedScope = scope.replace("/", "_").replace("-", "_");
        if (scope.startsWith("https://")) {
            labelResourceId += escapedScope.substring(8);
        } else {
            labelResourceId += escapedScope;
        }
        int labelResource = getResources().getIdentifier(labelResourceId, "string", getPackageName());
        if (labelResource != 0) {
            return getString(labelResource);
        }
        return "unknown";
    }

    private String getScopeDescription(String scope) {
        ConsentData.ScopeDetails matched = findScopeDetails(scope);
        return matched != null ? matched.description : null;
    }

    private ConsentData.ScopeDetails findScopeDetails(String scope) {
        if (data.consentData == null || scope == null) return null;
        String aliasFull = canonicalOidcScope(scope);
        for (ConsentData.ScopeDetails sd : data.consentData.scopes) {
            if (scope.equals(sd.id)) return sd;
            if (aliasFull != null && aliasFull.equals(sd.id)) return sd;
        }
        return null;
    }

    private static String canonicalOidcScope(String scope) {
        switch (scope) {
            case "email":
                return "https://www.googleapis.com/auth/userinfo.email";
            case "profile":
                return "https://www.googleapis.com/auth/userinfo.profile";
            default:
                return null;
        }
    }

    private String getServiceLabel(String service) {
        int labelResource = getResources().getIdentifier("permission_service_" + service + "_label", "string", getPackageName());
        if (labelResource != 0) {
            return getString(labelResource);
        }
        return "unknown";
    }

    private class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(AskPermissionActivity.this)
                    .inflate(R.layout.ask_permission_list_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = getItem(position);
            String label;
            String description;
            if (isOAuth()) {
                label = getScopeLabel(item);
                description = getScopeDescription(item);
            } else {
                label = getServiceLabel(item);
                description = null;
            }
            holder.text1.setText(label);
            if (description != null && !description.isEmpty()) {
                holder.text2.setText(Html.fromHtml(description.trim().replace("\n", "<br>")));
                holder.text2.setVisibility(VISIBLE);
            } else {
                holder.text2.setVisibility(GONE);
            }
        }

        @Override
        public int getItemCount() {
            if (isOAuth()) {
                return data.service.split(" ").length;
            }
            return 1;
        }

        public String getItem(int position) {
            if (isOAuth()) {
                String tokens = data.service.split(":", 2)[1];
                return tokens.split(" ")[position];
            }
            return data.service;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;
            ViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
            }
        }
    }
}