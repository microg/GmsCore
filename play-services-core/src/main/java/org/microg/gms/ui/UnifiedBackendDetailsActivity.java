package org.microg.gms.ui;

import androidx.fragment.app.Fragment;

import org.microg.nlp.ui.BackendDetailsFragment;
import org.microg.nlp.ui.BackendListFragment;
import org.microg.tools.ui.AbstractSettingsActivity;

public class UnifiedBackendDetailsActivity extends AbstractSettingsActivity {
    public UnifiedBackendDetailsActivity() {
        showHomeAsUp = true;
    }

    @Override
    protected Fragment getFragment() {
        return new BackendDetailsFragment();
    }
}
