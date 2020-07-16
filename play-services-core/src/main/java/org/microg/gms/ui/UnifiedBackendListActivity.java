package org.microg.gms.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.microg.nlp.ui.BackendListFragment;
import org.microg.tools.ui.AbstractSettingsActivity;

public class UnifiedBackendListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            Log.w("GmsCoreSettingUi", e);
        }
        getSupportFragmentManager().beginTransaction().add(new BackendListFragment(), null).commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
