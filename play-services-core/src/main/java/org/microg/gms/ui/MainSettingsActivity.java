package org.microg.gms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.BuildConfig;
import com.google.android.gms.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.microg.gms.common.Constants;
import org.microg.gms.ui.settings.SettingsProvider;

import static org.microg.gms.ui.UtilsKt.buildAlertDialog;
import static org.microg.gms.ui.settings.SettingsProviderKt.getAllSettingsProviders;

public class MainSettingsActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;

    private static final String FIRST_RUN_MASTER = "org.microg.gms_firstRun";
    private static final String FIRST_RUN_PREF = "as_run";

    private NavController getNavController() {
        return ((NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.navhost)).getNavController();
    }

    private void showDialogIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(FIRST_RUN_MASTER, MODE_PRIVATE);
        if (BuildConfig.APPLICATION_ID == Constants.USER_MICROG_PACKAGE_NAME &&
                prefs.getBoolean(FIRST_RUN_PREF, true)) {
            buildAlertDialog(this)
                    .setMessage(R.string.limited_services_dialog_information)
                    .setTitle(R.string.limited_services_app_name)
                    .setPositiveButton(R.string.limited_services_dialog_information_ack, (dialog, id) -> {
                        prefs.edit().putBoolean(FIRST_RUN_PREF, false).apply();
                    })
                    .create()
                    .show();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        for (SettingsProvider settingsProvider : getAllSettingsProviders(this)) {
            settingsProvider.preProcessSettingsIntent(intent);
        }

        setContentView(R.layout.settings_root_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.collapsing_toolbar);
        setSupportActionBar(toolbar);

        for (SettingsProvider settingsProvider : getAllSettingsProviders(this)) {
            settingsProvider.extendNavigation(getNavController());
        }

        appBarConfiguration = new AppBarConfiguration.Builder(getNavController().getGraph()).build();
        NavigationUI.setupWithNavController(toolbarLayout, toolbar, getNavController(), appBarConfiguration);
        showDialogIfNeeded();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(getNavController(), appBarConfiguration) || super.onSupportNavigateUp();
    }
}
