package org.microg.tools.updater;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.microg.tools.ui.BuildConfig;
import org.microg.tools.ui.R;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UpdateChecker {

    private static final String TAG = "UpdateChecker";

    private static final String GITHUB_API_URL = "https://api.github.com/repos/MorpheApp/MicroG-RE/releases";
    private static final String GITHUB_RELEASE_LINK = "https://github.com/MorpheApp/MicroG-RE/releases";
    private static final String MORPHE_RELEASE_LINK = "https://morphe.software/microg";

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

        new Thread(() -> {
            try {
                String version = fetchLatestVersion();
                mainHandler.post(() -> {
                    handleLatestVersion(version, view);
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception ex) {
                mainHandler.post(() -> {
                    Log.e(TAG, "Update check failed", ex);
                    showSnackbar(view, context.getString(R.string.update_checker_generic_error), false, null);
                    if (onComplete != null) onComplete.run();
                });
            }
        }).start();
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE) // Added in core module manifest, solved when an apk is generated
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @NonNull
    private String fetchLatestVersion() {
        boolean isDev = isDevVersion();
        String url = getReleaseUrl(GITHUB_API_URL, isDev);
        Request request = new Request.Builder().url(url).header("User-Agent", "MicroG-RE-Updater").build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            ResponseBody body = response.body();
            if (body == null) return "";
            String content = body.string();

            JSONObject json;
            if (isDev) {
                JSONArray jsonArray = new JSONArray(content);
                if (jsonArray.length() == 0) return "";
                json = jsonArray.getJSONObject(0);
            } else {
                json = new JSONObject(content);
            }
            String tagName = json.optString("tag_name", "").trim();
            if (tagName.startsWith("v")) {
                return tagName.substring(1);
            }
            return tagName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isDevVersion() {
        //noinspection ConstantConditions
        return BuildConfig.APP_VERSION_NAME.contains("dev");
    }

    private static String getReleaseUrl(String baseUrl, boolean isDev) {
        return isDev ? baseUrl : baseUrl + "/latest";
    }

    private void handleLatestVersion(@NonNull String latestVersion, @NonNull View view) {
        Context context = contextRef.get();
        if (context == null || latestVersion.isEmpty()) return;
        if (context instanceof Activity && ((Activity) context).isFinishing()) return;

        String currentVersion = BuildConfig.APP_VERSION_NAME;
        if (VersionUtils.compareVersions(currentVersion, latestVersion) < 0) {
            String message = context.getString(R.string.update_checker_update_available, latestVersion);
            String url = latestVersion.contains("dev") ? GITHUB_RELEASE_LINK : MORPHE_RELEASE_LINK;
            showSnackbar(view, message, true, v -> openReleaseLink(context, url));
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

    private void openReleaseLink(@NonNull Context context, @NonNull String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening release link", e);
        }
    }
}