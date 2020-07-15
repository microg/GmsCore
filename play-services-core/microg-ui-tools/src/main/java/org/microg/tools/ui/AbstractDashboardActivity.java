package org.microg.tools.ui;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDashboardActivity extends AppCompatActivity {
    protected int preferencesResource = 0;

    private final List<Condition> conditions = new ArrayList<Condition>();
    private ViewGroup conditionContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_activity);
        conditionContainer = (ViewGroup) findViewById(R.id.condition_container);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, getFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        forceConditionReevaluation();
    }

    private synchronized void resetConditionViews() {
        conditionContainer.removeAllViews();
        for (Condition condition : conditions) {
            if (condition.isEvaluated()) {
                if (condition.isActive(this)) {
                    addConditionToView(condition);
                }
            } else {
                evaluateConditionAsync(condition);
            }
        }
    }

    private void evaluateConditionAsync(final Condition condition) {
        if (condition.willBeEvaluating()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (condition.isActive(AbstractDashboardActivity.this)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (conditions.contains(condition) && condition.isEvaluated()) {
                                    addConditionToView(condition);
                                }
                            }
                        });
                    }
                }
            }).start();
        }
    }

    protected void forceConditionReevaluation() {
        for (Condition condition : conditions) {
            condition.resetEvaluated();
        }
        resetConditionViews();
    }

    protected void addAllConditions(Condition[] conditions) {
        for (Condition condition : conditions) {
            addCondition(condition);
        }
    }

    protected void addCondition(Condition condition) {
        conditions.add(condition);
        if (conditionContainer == null) return;
        if (condition.isEvaluated()) {
            addConditionToView(condition);
        } else {
            evaluateConditionAsync(condition);
        }
    }

    private synchronized void addConditionToView(Condition condition) {
        for (int i = 0; i < conditionContainer.getChildCount(); i++) {
            if (conditionContainer.getChildAt(i).getTag() == condition) return;
        }
        conditionContainer.addView(condition.createView(this, conditionContainer));
    }

    protected void clearConditions() {
        conditions.clear();
        resetConditionViews();
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
