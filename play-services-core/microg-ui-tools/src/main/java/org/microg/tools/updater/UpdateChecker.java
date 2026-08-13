package org.microg.tools.updater;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;
import org.microg.tools.ui.BuildConfig;
import org.microg.tools.ui.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UpdateChecker {

    private static final String TAG = "UpdateChecker";

    private static final String GITHUB_API_URL = "https://api.github.com/repos/MorpheApp/MicroG-RE/releases/latest";
    private static final String GITHUB_RELEASE_LINK = "https://github.com/MorpheApp/MicroG-RE/releases/latest";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();

    private final WeakReference<Context> contextRef;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public UpdateChecker(@NonNull Context context) {
        this.contextRef = new WeakReference<>(context);
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)  // Added in core module manifest, solved when an apk is generated
    public void checkForUpdates(@Nullable View view, @Nullable Runnable onComplete) {
        if (view == null) return;
        Context context = contextRef.get();
        if (context == null) return;

        if (!isNetworkAvailable(context)) {
            showSnackbar(view, context.getString(R.string.update_checker_no_internet), false, null);
            if (onComplete != null) onComplete.run();
            return;
        }

        CompletableFuture.supplyAsync(this::fetchLatestVersion).thenAccept(version -> mainHandler.post(() -> {
            handleLatestVersion(version, view);
            if (onComplete != null) onComplete.run();
        })).exceptionally(ex -> {
            mainHandler.post(() -> {
                Log.e(TAG, "Update check failed", ex);
                showSnackbar(view, context.getString(R.string.update_checker_generic_error), false, null);
                if (onComplete != null) onComplete.run();
            });
            return null;
        });
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE) // Added in core module manifest, solved when an apk is generated
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @NonNull
    private String fetchLatestVersion() {
        Request request = new Request.Builder().url(GITHUB_API_URL).header("User-Agent", "MicroG-RE-Updater").build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            ResponseBody body = response.body();

            JSONObject json = new JSONObject(body.string());
            return json.optString("tag_name", "").replace("v", "").trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleLatestVersion(@NonNull String latestVersion, @NonNull View view) {
        Context context = contextRef.get();
        if (context == null || latestVersion.isEmpty()) return;
        if (context instanceof Activity && ((Activity) context).isFinishing()) return;

        String currentVersion = BuildConfig.APP_VERSION_NAME;
        if (VersionUtils.compareVersions(currentVersion, latestVersion) < 0) {
            String message = context.getString(R.string.update_checker_update_available, latestVersion);
            showSnackbar(view, message, true, v -> openGitHubReleaseLink(context));
        } else {
            showSnackbar(view, context.getString(R.string.update_checker_no_update), false, null);
        }
    }

    private void showSnackbar(@NonNull View view, @NonNull String message, boolean isUpdate, @Nullable View.OnClickListener action) {
        if (!view.isAttachedToWindow()) return;

        int duration = isUpdate ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;
        Snackbar snackbar = Snackbar.make(view, message, duration);

        if (isUpdate && action != null) {
            snackbar.setAction(R.string.update_checker_download_button, action);
        }

        configureSnackbarInsets(snackbar);
        snackbar.show();
    }

    private void configureSnackbarInsets(@NonNull Snackbar snackbar) {
        View snackbarView = snackbar.getView();
        ViewCompat.setOnApplyWindowInsetsListener(snackbarView, (v, insets) -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            v.setLayoutParams(params);
            return insets;
        });
    }

    private void openGitHubReleaseLink(@NonNull Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASE_LINK)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening release link", e);
        }
    }
}