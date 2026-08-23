package org.microg.tools.selfcheck;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.R;

import org.microg.tools.ui.AbstractSelfCheckFragment;

import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Neutral;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;

public class PermissionsChecks implements SelfCheckGroup {

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_permissions);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        isSendNotificationPermissionGranted(context, collector);
        isOverlayPermissionGranted(context, collector);
    }

    private void isSendNotificationPermissionGranted(final Context context, ResultCollector collector) {
        boolean enabled = NotificationManagerCompat.from(context).areNotificationsEnabled();

        collector.addResult(
                context.getString(R.string.self_check_name_notifications),
                enabled ? Positive : Negative,
                context.getString(R.string.self_check_resolution_notifications),
                true, null,
                fragment -> {
                    Intent intent = new Intent();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                    } else {
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                    }
                    launch(fragment, intent);
                });
    }

    private void isOverlayPermissionGranted(final Context context, ResultCollector collector) {
        boolean canDraw = Settings.canDrawOverlays(context);

        collector.addResult(
                context.getString(R.string.self_check_name_overlay),
                canDraw ? Positive : Neutral,
                context.getString(R.string.self_check_resolution_overlay),
                true, null,
                fragment -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    launch(fragment, intent);
                });
    }

    private void launch(Fragment fragment, Intent intent) {
        if (fragment instanceof AbstractSelfCheckFragment) {
            ((AbstractSelfCheckFragment) fragment).launchIntent(intent);
        } else {
            fragment.startActivity(intent);
        }
    }
}
