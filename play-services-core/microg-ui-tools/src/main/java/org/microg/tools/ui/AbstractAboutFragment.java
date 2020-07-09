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

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class AbstractAboutFragment extends Fragment {

    protected abstract void collectLibraries(List<Library> libraries);

    public static Drawable getIcon(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo(context.getPackageName(), 0).applicationInfo.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            // Never happens, self package always exists!
            throw new RuntimeException(e);
        }
    }

    public static String getAppName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            CharSequence label = pm.getPackageInfo(context.getPackageName(), 0).applicationInfo.loadLabel(pm);
            if (TextUtils.isEmpty(label)) return context.getPackageName();
            return label.toString().trim();
        } catch (PackageManager.NameNotFoundException e) {
            // Never happens, self package always exists!
            throw new RuntimeException(e);
        }
    }

    protected String getAppName() {
        return getAppName(getContext());
    }

    public static String getLibVersion(String packageName) {
        try {
            String versionName = (String) Class.forName(packageName + ".BuildConfig").getField("VERSION_NAME").get(null);
            if (TextUtils.isEmpty(versionName)) return "";
            return versionName.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getSelfVersion(Context context) {
        return getLibVersion(context.getPackageName());
    }

    protected String getSelfVersion() {
        return getSelfVersion(getContext());
    }

    protected String getSummary() {
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View aboutRoot = inflater.inflate(R.layout.about_root, container, false);
        ((ImageView) aboutRoot.findViewById(android.R.id.icon)).setImageDrawable(getIcon(getContext()));
        ((TextView) aboutRoot.findViewById(android.R.id.title)).setText(getAppName());
        ((TextView) aboutRoot.findViewById(R.id.about_version)).setText(getString(R.string.about_version_str, getSelfVersion()));
        String summary = getSummary();
        if (summary != null) {
            ((TextView) aboutRoot.findViewById(android.R.id.summary)).setText(summary);
            aboutRoot.findViewById(android.R.id.summary).setVisibility(View.VISIBLE);
        }

        List<Library> libraries = new ArrayList<Library>();
        collectLibraries(libraries);
        Collections.sort(libraries);
        ((ListView) aboutRoot.findViewById(android.R.id.list)).setAdapter(new LibraryAdapter(getContext(), libraries.toArray(new Library[libraries.size()])));

        return aboutRoot;
    }

    private class LibraryAdapter extends ArrayAdapter<Library> {

        public LibraryAdapter(Context context, Library[] libraries) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, libraries);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            ((TextView) v.findViewById(android.R.id.text1)).setText(getString(R.string.about_name_version_str, getItem(position).name, getLibVersion(getItem(position).packageName)));
            ((TextView) v.findViewById(android.R.id.text2)).setText(getItem(position).copyright != null ? getItem(position).copyright : getString(R.string.about_default_license));
            return v;
        }
    }

    protected static class Library implements Comparable<Library> {
        private final String packageName;
        private final String name;
        private final String copyright;

        public Library(String packageName, String name, String copyright) {
            this.packageName = packageName;
            this.name = name;
            this.copyright = copyright;
        }

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
