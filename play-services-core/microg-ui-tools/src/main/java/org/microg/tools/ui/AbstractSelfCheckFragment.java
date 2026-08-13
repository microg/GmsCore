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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.transition.MaterialSharedAxis;

import org.microg.tools.selfcheck.SelfCheckGroup;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.View.INVISIBLE;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Neutral;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Unknown;

public abstract class AbstractSelfCheckFragment extends Fragment {
    private ViewGroup root;
    private ImageView statusShape;
    private MaterialCardView statusCard;
    private boolean hasFailures = false;

    protected ActivityResultLauncher<Intent> resolutionLauncher;
    protected ActivityResultLauncher<String[]> permissionsLauncher;

    public static class ChipInfo {
        public String label;
        public Drawable icon;
        public View.OnClickListener onClick;

        public ChipInfo(String label, Drawable icon, View.OnClickListener onClick) {
            this.label = label;
            this.icon = icon;
            this.onClick = onClick;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolutionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> reset(LayoutInflater.from(getContext())));
        permissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> reset(LayoutInflater.from(getContext())));

        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    public void launchIntent(Intent intent) {
        resolutionLauncher.launch(intent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View scrollRoot = inflater.inflate(R.layout.self_check, container, false);
        root = scrollRoot.findViewById(R.id.self_check_root);
        statusShape = scrollRoot.findViewById(R.id.self_check_status_shape);
        statusCard = scrollRoot.findViewById(R.id.self_check_status_card);

        reset(inflater);
        return scrollRoot;
    }

    protected abstract void prepareSelfCheckList(List<SelfCheckGroup> checks);

    protected void reset(LayoutInflater inflater) {
        if (root == null) return;

        hasFailures = false;
        updateStatusHeader();

        List<SelfCheckGroup> selfCheckGroupList = new ArrayList<>();
        prepareSelfCheckList(selfCheckGroupList);
        root.removeAllViews();
        for (SelfCheckGroup group : selfCheckGroupList) {
            View groupView = inflater.inflate(R.layout.self_check_group, root, false);
            ((TextView) groupView.findViewById(android.R.id.title)).setText(group.getGroupName(getContext()));

            ViewGroup viewGroup = groupView.findViewById(R.id.group_content);
            group.doChecks(getContext(), new GroupResultCollector(viewGroup));
            root.addView(groupView);
        }
    }

    private void updateStatusHeader() {
        if (statusShape == null || statusCard == null) return;

        statusShape.setImageResource(hasFailures ? R.drawable.ic_negative : R.drawable.ic_positive);

        int cardColorAttr = hasFailures ? R.attr.colorErrorContainer : R.attr.colorPrimaryContainer;

        int cardColor = MaterialColors.getColor(statusCard, cardColorAttr);
        statusCard.setCardBackgroundColor(cardColor);
    }

    private class GroupResultCollector implements SelfCheckGroup.ResultCollector {
        private final ViewGroup viewGroup;

        public GroupResultCollector(ViewGroup viewGroup) {
            this.viewGroup = viewGroup;
        }

        @Override
        public void addResult(String name, SelfCheckGroup.Result result, String resolution) {
            addResult(name, result, resolution, true, null, null);
        }

        @Override
        public void addResult(String name, SelfCheckGroup.Result result, String resolution, SelfCheckGroup.CheckResolver resolver) {
            addResult(name, result, resolution, true, null, resolver);
        }

        @Override
        public void addResult(String name, SelfCheckGroup.Result result, String resolution, boolean showIcon, List<ChipInfo> chips, SelfCheckGroup.CheckResolver resolver) {
            if (getActivity() == null || getContext() == null) return;

            if (result == Negative && showIcon) {
                hasFailures = true;
            }

            getActivity().runOnUiThread(() -> {
                View entry = LayoutInflater.from(getContext()).inflate(R.layout.self_check_entry, viewGroup, false);

                TextView nameView = entry.findViewById(R.id.self_check_name);
                TextView resView = entry.findViewById(R.id.self_check_resolution);
                ImageView resultIcon = entry.findViewById(R.id.self_check_result_icon);
                ChipGroup chipGroup = entry.findViewById(R.id.self_check_chip_group);

                nameView.setText(name);

                if (showIcon) {
                    resultIcon.setVisibility(VISIBLE);
                    if (result == Positive) {
                        resultIcon.setImageResource(R.drawable.ic_positive);
                    } else if (result == Negative) {
                        resultIcon.setImageResource(R.drawable.ic_negative);
                    } else if (result == Neutral) {
                        resultIcon.setImageResource(R.drawable.ic_neutral);
                    } else {
                        resultIcon.setVisibility(INVISIBLE);
                    }
                } else {
                    resultIcon.setVisibility(GONE);
                }

                if (result == Positive) {
                    resView.setVisibility(GONE);
                } else {
                    resView.setVisibility(VISIBLE);
                    resView.setText(Html.fromHtml(resolution, Html.FROM_HTML_MODE_COMPACT));
                    if (resolver != null) {
                        entry.setClickable(true);
                        entry.setOnClickListener(v -> resolver.tryResolve(AbstractSelfCheckFragment.this));
                    }
                }

                if (chips != null && !chips.isEmpty()) {
                    chipGroup.setVisibility(VISIBLE);
                    chipGroup.removeAllViews();
                    for (ChipInfo info : chips) {
                        Chip chip = (Chip) LayoutInflater.from(getContext()).inflate(R.layout.self_check_chip, chipGroup, false);
                        chip.setText(info.label);
                        if (info.icon != null) {
                            chip.setChipIcon(info.icon);
                            chip.setChipIconVisible(true);
                        }
                        if (info.onClick != null) {
                            chip.setOnClickListener(info.onClick);
                        }
                        chipGroup.addView(chip);
                    }
                }

                viewGroup.addView(entry);
                updateSegmentedStyle();
                updateStatusHeader();
            });
        }

        private void updateSegmentedStyle() {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof com.google.android.material.listitem.ListItemLayout) {
                    ((com.google.android.material.listitem.ListItemLayout) child).updateAppearance(i, viewGroup.getChildCount());
                }
            }
        }
    }
}