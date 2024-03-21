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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.microg.tools.selfcheck.SelfCheckGroup;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Unknown;

public abstract class AbstractSelfCheckFragment extends Fragment {
    private static final String TAG = "SelfCheck";

    private ViewGroup root;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View scrollRoot = inflater.inflate(R.layout.self_check, container, false);
        root = (ViewGroup) scrollRoot.findViewById(R.id.self_check_root);
        reset(inflater);
        return scrollRoot;
    }

    protected abstract void prepareSelfCheckList(Context context, List<SelfCheckGroup> checks);

    protected void reset(LayoutInflater inflater) {
        List<SelfCheckGroup> selfCheckGroupList = new ArrayList<SelfCheckGroup>();
        prepareSelfCheckList(getContext(), selfCheckGroupList);

        root.removeAllViews();
        for (SelfCheckGroup group : selfCheckGroupList) {
            View groupView = inflater.inflate(R.layout.self_check_group, root, false);
            ((TextView) groupView.findViewById(android.R.id.title)).setText(group.getGroupName(getContext()));
            final ViewGroup viewGroup = (ViewGroup) groupView.findViewById(R.id.group_content);
            final SelfCheckGroup.ResultCollector collector = new GroupResultCollector(viewGroup);
            try {
                group.doChecks(getContext(), collector);
            } catch (Exception e) {
                Log.w(TAG, "Failed during check " + group.getGroupName(getContext()), e);
                collector.addResult("Self-check failed:", Negative, "An exception occurred during self-check. Please report this issue.");
            }
            root.addView(groupView);
        }
    }

    private class GroupResultCollector implements SelfCheckGroup.ResultCollector {
        private final ViewGroup viewGroup;

        public GroupResultCollector(ViewGroup viewGroup) {
            this.viewGroup = viewGroup;
        }

        @Override
        public void addResult(final String name, final SelfCheckGroup.Result result, final String resolution) {
            addResult(name, result, resolution, null);
        }

        @Override
        public void addResult(final String name, final SelfCheckGroup.Result result, final String resolution,
                              final SelfCheckGroup.CheckResolver resolver) {
            if (result == null || getActivity() == null) return;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View resultEntry = LayoutInflater.from(getContext()).inflate(R.layout.self_check_entry, viewGroup, false);
                    ((TextView) resultEntry.findViewById(R.id.self_check_name)).setText(name);
                    resultEntry.findViewById(R.id.self_check_result).setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return true;
                        }
                    });
                    if (result == Positive) {
                        ((CheckBox) resultEntry.findViewById(R.id.self_check_result)).setChecked(true);
                        resultEntry.findViewById(R.id.self_check_resolution).setVisibility(GONE);
                    } else {
                        ((TextView) resultEntry.findViewById(R.id.self_check_resolution)).setText(resolution);
                        if (result == Unknown) {
                            resultEntry.findViewById(R.id.self_check_result).setVisibility(INVISIBLE);
                        }
                        if (resolver != null) {
                            resultEntry.setClickable(true);
                            resultEntry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    resolver.tryResolve(AbstractSelfCheckFragment.this);
                                }
                            });
                        }
                    }
                    viewGroup.addView(resultEntry);
                }
            });
        }
    }
}
