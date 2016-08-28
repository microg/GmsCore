/*
 * Copyright 2013-2015 microG Project Team
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

package org.microg.gms.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.R;

import org.microg.gms.gcm.GcmData;
import org.microg.gms.gcm.PushRegisterService;

import java.util.ArrayList;

public class GcmRegisteredAppsFragment extends Fragment {

    private AppsAdapter appsAdapter = null;
    private GcmData gcmStorage = null;

    @Override
    @NonNull
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        gcmStorage = new GcmData(getContext());

        View view = inflater.inflate(R.layout.gcm_apps_list, container, false);
        ListView listView = (ListView) view.findViewById(R.id.list_view);
        registerForContextMenu(listView);
        listView.setAdapter(updateListView());
        return listView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ListView) getView().findViewById(R.id.list_view)).setAdapter(updateListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        GcmData.AppInfo appInfo = appsAdapter.getItem(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
        MenuInflater menuInflater = getActivity().getMenuInflater();
        menuInflater.inflate(R.menu.gcm_app, menu);

        PackageManager packageManager = getContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(appInfo.app, 0);
            menu.setHeaderTitle(packageManager.getApplicationLabel(applicationInfo));
        } catch (PackageManager.NameNotFoundException e) {
            menu.setHeaderTitle(appInfo.app);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final GcmData.AppInfo appInfo = appsAdapter.getItem(info.position);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                PushRegisterService.unregister(getContext(), appInfo.app, appInfo.appSignature, null, null);
                return null;
            }

            protected void onPostExecute(Void result) {
                ((ListView) getView().findViewById(R.id.list_view)).setAdapter(updateListView());
            }
        }.execute();
        return true;
    }

    synchronized public AppsAdapter updateListView() {
        ArrayList<GcmData.AppInfo> registeredApps = new ArrayList<GcmData.AppInfo>();
        for (GcmData.AppInfo appInfo : gcmStorage.getAppsInfo()) {
            if (appInfo.isRegistered()) {
                registeredApps.add(appInfo);
            }
        }
        appsAdapter = new AppsAdapter(getContext(), registeredApps.toArray(new GcmData.AppInfo[registeredApps.size()]));
        return appsAdapter;
    }

    private class AppsAdapter extends ArrayAdapter<GcmData.AppInfo> {

        public AppsAdapter(Context context, GcmData.AppInfo[] libraries) {
            super(context, R.layout.gcm_app, R.id.title, libraries);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            ImageView image = (ImageView) v.findViewById(R.id.image);
            TextView title = (TextView) v.findViewById(R.id.title);
            TextView sub = (TextView) v.findViewById(R.id.sub);
            TextView warning = (TextView) v.findViewById(R.id.warning);

            GcmData.AppInfo appInfo = getItem(position);
            PackageManager packageManager = getContext().getPackageManager();
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(appInfo.app, 0);
                title.setText(packageManager.getApplicationLabel(applicationInfo).toString());
                image.setImageDrawable(packageManager.getApplicationIcon(applicationInfo));
                if (appInfo.hasUnregistrationError()) {
                    warning.setVisibility(View.VISIBLE);
                    warning.setText(getString(R.string.gcm_app_error_unregistering));
                }
            } catch (PackageManager.NameNotFoundException e) {
                title.setText(appInfo.app);
                warning.setVisibility(View.VISIBLE);
                warning.setText(getString(R.string.gcm_app_not_installed_anymore));
            }
            sub.setText(getString(R.string.gcm_messages_received_no, gcmStorage.getAppMessageCount(appInfo.app)));

            return v;
        }
    }
}