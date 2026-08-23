package org.microg.tools.selfcheck;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.gms.R;

import org.microg.gms.utils.AppPatcherDetector;
import org.microg.tools.ui.AbstractSelfCheckFragment.ChipInfo;

import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;

import java.util.ArrayList;
import java.util.List;

public class InstalledPatcherChecks implements SelfCheckGroup {

    private static final String MORPHE_PACKAGE_SUBSTRING = ".morphe.android";

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_patched_apps);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(0);

        List<ChipInfo> morpheChips = new ArrayList<>();
        List<ChipInfo> alternativeChips = new ArrayList<>();
        List<String> morpheNames = new ArrayList<>();
        List<String> alternativeNames = new ArrayList<>();

        for (PackageInfo pkg : installedPackages) {
            String pkgName = pkg.packageName;
            Integer resId = AppPatcherDetector.INSTANCE.getUsingPackageName(pkgName);

            if (resId != null && pkg.applicationInfo != null) {
                String label = pkg.applicationInfo.loadLabel(pm).toString();
                Drawable icon = pkg.applicationInfo.loadIcon(pm);
                View.OnClickListener openAppListener = v -> {
                    Intent intent = pm.getLaunchIntentForPackage(pkgName);
                    if (intent != null) context.startActivity(intent);
                };

                ChipInfo chip = new ChipInfo(label, icon, openAppListener);

                if (pkgName.contains(MORPHE_PACKAGE_SUBSTRING)) {
                    if (!morpheNames.contains(label)) {
                        morpheChips.add(chip);
                        morpheNames.add(label);
                    }
                } else {
                    if (!alternativeNames.contains(label)) {
                        alternativeChips.add(chip);
                        alternativeNames.add(label);
                    }
                }
            }
        }

        processMorpheResult(context, collector, morpheChips, morpheNames);

        if (!alternativeChips.isEmpty()) {
            processAlternativeResult(context, collector, alternativeChips, alternativeNames);
        }
    }

    private void processMorpheResult(Context context, ResultCollector collector, List<ChipInfo> chips, List<String> names) {
        boolean isPositive = !chips.isEmpty();
        String sourceName = context.getString(R.string.self_check_source_morphe);
        String title = context.getString(R.string.self_check_patched_app_installed, sourceName);
        String resolution = isPositive ? "" : context.getString(R.string.self_check_resolution_patched_app_installed, sourceName);

        if (!isPositive) {
            Drawable downloadIcon = ContextCompat.getDrawable(context, R.drawable.ic_download);
            String downloadText = context.getString(R.string.self_check_action_download_morphe);
            chips.add(new ChipInfo(downloadText, downloadIcon, v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://morphe.software/"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }));
        }

        collector.addResult(title, isPositive ? Positive : Negative, resolution, false, chips, null);
    }

    private void processAlternativeResult(Context context, ResultCollector collector, List<ChipInfo> chips, List<String> names) {
        String sourceName = context.getString(R.string.self_check_source_alternative);
        String title = context.getString(R.string.self_check_patched_app_installed, sourceName);
        collector.addResult(title, Positive, "", false, chips, null);
    }
}