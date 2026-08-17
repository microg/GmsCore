/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.tools.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.listitem.ListItemLayout;
import com.google.android.material.transition.MaterialSharedAxis;

import org.microg.tools.updater.UpdateChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @noinspection unused
 */
public abstract class AbstractAboutFragment extends Fragment {

    protected abstract void collectLibraries(List<Library> libraries);

//    public static Drawable getIcon(Context context) {
//        try {
//            PackageManager pm = context.getPackageManager();
//            return Objects.requireNonNull(pm.getPackageInfo(context.getPackageName(), 0).applicationInfo).loadIcon(pm);
//        } catch (PackageManager.NameNotFoundException e) {
//            // Never happens, self package always exists!
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static String getAppName(Context context) {
//        try {
//            PackageManager pm = context.getPackageManager();
//            CharSequence label = Objects.requireNonNull(pm.getPackageInfo(context.getPackageName(), 0).applicationInfo).loadLabel(pm);
//            if (TextUtils.isEmpty(label)) return context.getPackageName();
//            return label.toString().trim();
//        } catch (PackageManager.NameNotFoundException e) {
//            // Never happens, self package always exists!
//            throw new RuntimeException(e);
//        }
//    }
//
//    protected String getAppName() {
//        return getAppName(requireContext());
//    }

    public static String getLibVersion(String packageName) {
        try {
            String versionName = (String) Class.forName(packageName + ".BuildConfig").getField("VERSION_NAME").get(null);
            if (TextUtils.isEmpty(versionName)) return "";
            return versionName.trim();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(MaterialColors.getColor(view, android.R.attr.colorBackground));
    }

    public static String getAppVersion() {
        return BuildConfig.APP_VERSION_NAME;
    }

    public static String getGmsVersion() {
        return BuildConfig.GMS_VERSION_NAME;
    }

    public static String getAppVersion(Context context) {
        return getAppVersion();
    }

    public static String getGmsVersion(Context context) {
        return getGmsVersion();
    }

    protected String getSummary() {
        return null;
    }

    @Nullable
    @Override
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE) // (UpdateChecker) Added in core module manifest, solved when an apk is generated
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View aboutRoot = inflater.inflate(R.layout.about_root, container, false);

        ViewGroup appCardContainer = aboutRoot.findViewById(R.id.app_card_container);
        if (appCardContainer != null) {
            View appCard = inflater.inflate(R.layout.about_app, appCardContainer, true);
//            ((ImageView) appCard.findViewById(R.id.app_icon)).setImageDrawable(getIcon(requireContext()));
//            ((TextView) appCard.findViewById(R.id.app_title)).setText(getAppName());
            ((TextView) appCard.findViewById(R.id.app_version)).setText(appCard.getContext().getString(R.string.about_version_str, getAppVersion()));

            appCard.findViewById(R.id.app_check_updates).setOnClickListener(v -> {
                new UpdateChecker(requireContext()).checkForUpdates(v, () -> {
                });
            });

            View appInfo = appCard.findViewById(R.id.app_info);
            if (appInfo != null) {
                appInfo.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    try {
                        startActivity(intent);
                    } catch (Exception ignored) {
                    }
                });
            }
        }

        ViewGroup morpheCardContainer = aboutRoot.findViewById(R.id.morphe_card_container);
        if (morpheCardContainer != null) {
            View morpheCard = inflater.inflate(R.layout.about_morphe, morpheCardContainer, true);

            morpheCard.findViewById(R.id.morphe_github).setOnClickListener(v -> openUrl("https://github.com/MorpheApp"));
            morpheCard.findViewById(R.id.morphe_x).setOnClickListener(v -> openUrl("https://twitter.com/MorpheApp"));
            morpheCard.findViewById(R.id.morphe_reddit).setOnClickListener(v -> openUrl("https://www.reddit.com/r/MorpheApp"));
            morpheCard.findViewById(R.id.morphe_website).setOnClickListener(v -> openUrl("https://morphe.software/"));
        }

        List<Library> libraries = new ArrayList<>();
        collectLibraries(libraries);
        Collections.sort(libraries);

        ViewGroup libraryContainer = aboutRoot.findViewById(R.id.library_container);
        if (libraryContainer != null) {
            for (int i = 0; i < libraries.size(); i++) {
                Library library = libraries.get(i);
                View libraryView = inflater.inflate(R.layout.library_item, libraryContainer, false);

                TextView title = libraryView.findViewById(android.R.id.text1);
                TextView subtitle = libraryView.findViewById(android.R.id.text2);

                title.setText(getString(R.string.about_name_version_str, library.name, getLibVersion(library.packageName)));
                subtitle.setText(library.copyright != null ? library.copyright : getString(R.string.about_default_license));

                ListItemLayout listItemLayout = libraryView.findViewById(R.id.list_item_library);
                if (listItemLayout != null) {
                    listItemLayout.updateAppearance(i, libraries.size());
                }

                libraryContainer.addView(libraryView);
            }
        }

        return aboutRoot;
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private class LibraryAdapter extends ArrayAdapter<Library> {
        public LibraryAdapter(Context context, Library[] libraries) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, libraries);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            ((TextView) v.findViewById(android.R.id.text1)).setText(getString(R.string.about_name_version_str, Objects.requireNonNull(getItem(position)).name, getLibVersion(Objects.requireNonNull(getItem(position)).packageName)));
            ((TextView) v.findViewById(android.R.id.text2)).setText(Objects.requireNonNull(getItem(position)).copyright != null ? Objects.requireNonNull(getItem(position)).copyright : getString(R.string.about_default_license));
            return v;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    protected static class Library implements Comparable<Library> {
        public final String packageName;
        public final String name;
        public final String copyright;

        public Library(String packageName, String name, String copyright) {
            this.packageName = packageName;
            this.name = name;
            this.copyright = copyright;
        }

        @NonNull
        @Override
        public String toString() {
            return name + ", " + copyright;
        }

        @Override
        public int compareTo(Library another) {
            return name.toLowerCase(Locale.US).compareTo(another.name.toLowerCase(Locale.US));
        }
    }
}