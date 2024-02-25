package org.microg.gms.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import org.microg.gms.ui.settings.SettingsProvider;

import static org.microg.gms.ui.settings.SettingsProviderKt.getAllSettingsProviders;

public class MainSettingsActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;

    private NavController getNavController() {
        return ((NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.navhost)).getNavController();
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
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(getNavController(), appBarConfiguration) || super.onSupportNavigateUp();
    }
}
