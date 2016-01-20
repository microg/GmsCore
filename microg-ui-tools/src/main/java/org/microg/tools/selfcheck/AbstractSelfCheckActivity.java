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

package org.microg.tools.selfcheck;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.microg.tools.ui.R;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSelfCheckActivity extends AppCompatActivity implements SelfCheckGroup.ResultCollector {

    protected abstract void prepareSelfCheckList(List<SelfCheckGroup> checks);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.self_check);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        List<SelfCheckGroup> selfCheckGroupList = new ArrayList<SelfCheckGroup>();
        prepareSelfCheckList(selfCheckGroupList);

        for (SelfCheckGroup group : selfCheckGroupList) {
            group.doChecks(this, this);
        }
    }

    public void addResult(String name, SelfCheckGroup.Result result, String resolution) {
        if (result == null) return;
        ViewGroup root = (ViewGroup) findViewById(R.id.self_check_root);
        View resultEntry = LayoutInflater.from(this).inflate(R.layout.self_check_entry, root, false);
        ((TextView) resultEntry.findViewById(R.id.self_check_name)).setText(name);
        if (result == SelfCheckGroup.Result.Positive) {
            ((ImageView) resultEntry.findViewById(R.id.self_check_result)).setImageResource(android.R.drawable.presence_online);
        } else {
            ((TextView) resultEntry.findViewById(R.id.self_check_resolution)).setText(resolution);
            ((ImageView) resultEntry.findViewById(R.id.self_check_result))
                    .setImageResource(result == SelfCheckGroup.Result.Negative ? android.R.drawable.presence_busy : android.R.drawable.presence_invisible);
        }
        root.addView(resultEntry);
    }
}
