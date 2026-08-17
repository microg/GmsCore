package org.microg.tools.ui;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Objects;

/**
 * @noinspection RedundantCast, unused
 */
public abstract class AbstractSettingsActivity extends AppCompatActivity {
    protected boolean showHomeAsUp = false;
    protected int preferencesResource = 0;
    private ViewGroup customBarContainer;
    protected int customBarLayout = 0;
    protected SwitchBar switchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        if (showHomeAsUp) {
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        }

        switchBar = (SwitchBar) findViewById(R.id.switch_bar);

        customBarContainer = (ViewGroup) findViewById(R.id.custom_bar);
        if (customBarLayout != 0) {
            customBarContainer.addView(getLayoutInflater().inflate(customBarLayout, customBarContainer, false));
        }

        setupWindowInsets();

        getSupportFragmentManager().beginTransaction().replace(R.id.content_wrapper, getFragment()).commit();
    }

    private void setupWindowInsets() {
        View rootLayout = findViewById(R.id.root_layout);
        ExtendedFloatingActionButton fab = findViewById(R.id.preference_fab);
        NestedScrollView nestedScrollView = findViewById(R.id.nested_scroll_view);

        final int initialScrollViewPaddingLeft = nestedScrollView.getPaddingLeft();
        final int initialScrollViewPaddingTop = nestedScrollView.getPaddingTop();
        final int initialScrollViewPaddingRight = nestedScrollView.getPaddingRight();
        final int initialScrollViewPaddingBottom = nestedScrollView.getPaddingBottom();

        ViewGroup.MarginLayoutParams fabInitialParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
        final int initialFabMarginLeft = fabInitialParams.leftMargin;
        final int initialFabMarginRight = fabInitialParams.rightMargin;
        final int initialFabMarginBottom = fabInitialParams.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

            boolean imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());
            int bottomInset = imeVisible ? imeInsets.bottom : systemBarsInsets.bottom;

            nestedScrollView.setPadding(initialScrollViewPaddingLeft + systemBarsInsets.left, initialScrollViewPaddingTop, initialScrollViewPaddingRight + systemBarsInsets.right, initialScrollViewPaddingBottom + bottomInset);

            ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            fabParams.leftMargin = initialFabMarginLeft + systemBarsInsets.left;
            fabParams.rightMargin = initialFabMarginRight + systemBarsInsets.right;
            fabParams.bottomMargin = initialFabMarginBottom + systemBarsInsets.bottom;
            fab.setLayoutParams(fabParams);

            return windowInsets;
        });

        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > oldScrollY) {
                fab.shrink();
            } else if (scrollY < oldScrollY) {
                fab.extend();
            }
        });
    }

    private void enableEdgeToEdgeNoContrast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EdgeToEdge.enable(this, SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    public void setCustomBarLayout(int layout) {
        customBarLayout = layout;
        if (customBarContainer != null) {
            customBarContainer.removeAllViews();
            customBarContainer.addView(getLayoutInflater().inflate(customBarLayout, customBarContainer, false));
        }
    }

    public SwitchBar getSwitchBar() {
        return switchBar;
    }

    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().addToBackStack("root").setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).replace(R.id.content_wrapper, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected Fragment getFragment() {
        if (preferencesResource == 0) {
            throw new IllegalStateException("Neither preferencesResource given, nor overriden getFragment()");
        }
        ResourceSettingsFragment fragment = new ResourceSettingsFragment();
        Bundle b = new Bundle();
        b.putInt(ResourceSettingsFragment.EXTRA_PREFERENCE_RESOURCE, preferencesResource);
        fragment.setArguments(b);
        return fragment;
    }
}