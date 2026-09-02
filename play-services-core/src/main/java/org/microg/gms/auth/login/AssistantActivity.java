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

package org.microg.gms.auth.login;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.R;

import java.util.Objects;

public abstract class AssistantActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdgeNoContrast();
        setContentView(R.layout.login_assistant);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //noinspection deprecation
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        findViewById(R.id.spoof_button).setOnClickListener(v -> onHuaweiButtonClicked());
        findViewById(R.id.next_button).setOnClickListener(v -> onNextButtonClicked());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth_root), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void setSpoofButtonText(@StringRes int res) {
        setSpoofButtonText(getText(res));
    }

    public void setSpoofButtonText(CharSequence text) {
        if (text == null) {
            findViewById(R.id.spoof_button).setVisibility(View.GONE);
        } else {
            findViewById(R.id.spoof_button).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.spoof_button)).setText(text);
        }
    }

    public void setNextButtonText(@StringRes int res) {
        setNextButtonText(getText(res));
    }

    public void setNextButtonText(CharSequence text) {
        if (text == null) {
            findViewById(R.id.next_button).setVisibility(View.GONE);
        } else {
            findViewById(R.id.next_button).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.next_button)).setText(text);
        }
    }

    protected void onHuaweiButtonClicked() {
    }

    protected void onNextButtonClicked() {
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        ((TextView) findViewById(R.id.title)).setText(title);
    }

    /** @noinspection unused*/
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private void enableEdgeToEdgeNoContrast() {
        SystemBarStyle systemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(this, systemBarStyle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }
}
