package org.microg.gms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import static org.microg.gms.ui.settings.SettingsProviderKt.getAllSettingsProviders;

public class MainSettingsActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;

    private static final String FirstRunMaster = "FirstRun";
    private static final String FirstRunPref = "FirstRun_pref";

    private NavController getNavController() {
        return ((NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.navhost)).getNavController();
    }

    private void showDialogIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(FirstRunMaster, MODE_PRIVATE);
        if (BuildConfig.APPLICATION_ID == Constants.MICROG_PACKAGE_NAME &&
                prefs.getBoolean(FirstRunPref, true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.limited_services_dialog_information)
                    .setTitle(R.string.limited_services_app_name)
                    .setPositiveButton(R.string.limited_services_dialog_information_ack, (dialog, id) -> {
                        prefs.edit().putBoolean(FirstRunPref, false).apply();
                    });

            builder.create().show();
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
