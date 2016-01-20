/*
 * Copyright 2013-2016 microG Project Team
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

package android.support.v4.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public abstract class PreferenceFragment extends Fragment {

    private static final int FIRST_REQUEST_CODE = 100;
    private static final int MSG_BIND_PREFERENCES = 1;
    private static final String PREFERENCES_TAG = "android:preferences";
    private boolean mHavePrefs;
    private boolean mInitDone;
    private ListView mList;
    private PreferenceManager mPreferenceManager;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_BIND_PREFERENCES:
                    bindPreferences();
                    break;
            }
        }
    };

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    private void bindPreferences() {
        PreferenceScreen localPreferenceScreen = getPreferenceScreen();
        if (localPreferenceScreen != null) {
            ListView localListView = getListView();
            localPreferenceScreen.bind(localListView);
        }
    }

    private void ensureList() {
        if (mList == null) {
            View view = getView();
            if (view == null) {
                throw new IllegalStateException("Content view not yet created");
            }

            View listView = view.findViewById(android.R.id.list);
            if (!(listView instanceof ListView)) {
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' that is not a ListView class");
            }

            mList = (ListView) listView;
            if (mList == null) {
                throw new RuntimeException(
                        "Your content must have a ListView whose id attribute is 'android.R.id.list'");
            }

            mHandler.post(mRequestFocus);
        }
    }

    private void postBindPreferences() {
        if (mHandler.hasMessages(MSG_BIND_PREFERENCES)) {
            mHandler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget();
        }
    }

    private void requirePreferenceManager() {
        if (this.mPreferenceManager == null) {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
    }

    public void addPreferencesFromIntent(Intent intent) {
        requirePreferenceManager();
        PreferenceScreen screen = inflateFromIntent(intent, getPreferenceScreen());
        setPreferenceScreen(screen);
    }

    public void addPreferencesFromResource(int resId) {
        requirePreferenceManager();
        PreferenceScreen screen = inflateFromResource(getActivity(), resId, getPreferenceScreen());
        setPreferenceScreen(screen);
    }

    public Preference findPreference(CharSequence key) {
        if (mPreferenceManager == null) {
            return null;
        }
        return mPreferenceManager.findPreference(key);
    }

    public ListView getListView() {
        ensureList();
        return mList;
    }

    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setScrollBarStyle(0);
        if (mHavePrefs) {
            bindPreferences();
        }
        mInitDone = true;
        if (savedInstanceState != null) {
            Bundle localBundle = savedInstanceState.getBundle(PREFERENCES_TAG);
            if (localBundle != null) {
                PreferenceScreen screen = getPreferenceScreen();
                if (screen != null) {
                    screen.restoreHierarchyState(localBundle);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dispatchActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        mPreferenceManager = createPreferenceManager();
    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup,
                             Bundle paramBundle) {
        ListView listView = new ListView(paramLayoutInflater.getContext());
        listView.setId(android.R.id.list);
        listView.setDrawSelectorOnTop(false);
        listView.setPadding(12, 6, 12, 0);
        //listView.setSelector(null);
        return listView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dispatchActivityDestroy();
    }

    @Override
    public void onDestroyView() {
        mList = null;
        mHandler.removeCallbacks(mRequestFocus);
        mHandler.removeMessages(MSG_BIND_PREFERENCES);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            Bundle localBundle = new Bundle();
            screen.saveHierarchyState(localBundle);
            bundle.putBundle(PREFERENCES_TAG, localBundle);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        dispatchActivityStop();
    }

    /**
     * Access methods with visibility private *
     */

    private PreferenceManager createPreferenceManager() {
        try {
            Constructor<PreferenceManager> c = PreferenceManager.class
                    .getDeclaredConstructor(Activity.class, int.class);
            c.setAccessible(true);
            return c.newInstance(this.getActivity(), FIRST_REQUEST_CODE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PreferenceScreen getPreferenceScreen() {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("getPreferenceScreen");
            m.setAccessible(true);
            return (PreferenceScreen) m.invoke(mPreferenceManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        try {
            Method m = PreferenceManager.class
                    .getDeclaredMethod("setPreferences", PreferenceScreen.class);
            m.setAccessible(true);
            boolean result = (Boolean) m.invoke(mPreferenceManager, preferenceScreen);
            if (result && preferenceScreen != null) {
                mHavePrefs = true;
                if (mInitDone) {
                    postBindPreferences();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            Method m = PreferenceManager.class
                    .getDeclaredMethod("dispatchActivityResult", int.class, int.class,
                            Intent.class);
            m.setAccessible(true);
            m.invoke(mPreferenceManager, requestCode, resultCode, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void dispatchActivityDestroy() {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityDestroy");
            m.setAccessible(true);
            m.invoke(mPreferenceManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void dispatchActivityStop() {
        try {
            Method m = PreferenceManager.class.getDeclaredMethod("dispatchActivityStop");
            m.setAccessible(true);
            m.invoke(mPreferenceManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PreferenceScreen inflateFromResource(Context context, int resId,
                                                PreferenceScreen rootPreferences) {
        PreferenceScreen preferenceScreen;
        try {
            Method m = PreferenceManager.class
                    .getDeclaredMethod("inflateFromResource", Context.class, int.class,
                            PreferenceScreen.class);
            m.setAccessible(true);
            preferenceScreen = (PreferenceScreen) m
                    .invoke(mPreferenceManager, context, resId, rootPreferences);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return preferenceScreen;
    }

    public PreferenceScreen inflateFromIntent(Intent queryIntent,
                                              PreferenceScreen rootPreferences) {
        PreferenceScreen preferenceScreen;
        try {
            Method m = PreferenceManager.class
                    .getDeclaredMethod("inflateFromIntent", Intent.class, PreferenceScreen.class);
            m.setAccessible(true);
            preferenceScreen = (PreferenceScreen) m
                    .invoke(mPreferenceManager, queryIntent, rootPreferences);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return preferenceScreen;
    }
}
