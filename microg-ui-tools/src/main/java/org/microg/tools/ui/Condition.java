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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

public class Condition {
    @DrawableRes
    private final int iconRes;
    private final Drawable icon;

    @StringRes
    private final int titleRes;
    @PluralsRes
    private final int titlePluralsRes;
    private final CharSequence title;

    @StringRes
    private final int summaryRes;
    @PluralsRes
    private final int summaryPluralsRes;
    private final CharSequence summary;

    @StringRes
    private final int firstActionTextRes;
    @PluralsRes
    private final int firstActionPluralsRes;
    private final CharSequence firstActionText;
    private final View.OnClickListener firstActionListener;

    @StringRes
    private final int secondActionTextRes;
    @PluralsRes
    private final int secondActionPluralsRes;
    private final CharSequence secondActionText;
    private final View.OnClickListener secondActionListener;

    private final Evaluation evaluation;

    private boolean evaluated = false;
    private boolean evaluating = false;
    private int evaluatedPlurals = -1;
    private boolean active;

    Condition(Builder builder) {
        icon = builder.icon;
        title = builder.title;
        summary = builder.summary;
        firstActionText = builder.firstActionText;
        firstActionListener = builder.firstActionListener;
        secondActionText = builder.secondActionText;
        secondActionListener = builder.secondActionListener;
        summaryRes = builder.summaryRes;
        iconRes = builder.iconRes;
        firstActionTextRes = builder.firstActionTextRes;
        secondActionTextRes = builder.secondActionTextRes;
        titleRes = builder.titleRes;
        evaluation = builder.evaluation;
        titlePluralsRes = builder.titlePluralsRes;
        summaryPluralsRes = builder.summaryPluralsRes;
        firstActionPluralsRes = builder.firstActionPluralsRes;
        secondActionPluralsRes = builder.secondActionPluralsRes;
    }

    View createView(final Context context, ViewGroup container) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.condition_card, container, false);
        Drawable icon = getIcon(context);
        if (icon != null)
            ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(icon);
        ((TextView) view.findViewById(android.R.id.title)).setText(getTitle(context));
        ((TextView) view.findViewById(android.R.id.summary)).setText(getSummary(context));
        Button first = (Button) view.findViewById(R.id.first_action);
        first.setText(getFirstActionText(context));
        first.setOnClickListener(getFirstActionListener());
        CharSequence secondActionText = getSecondActionText(context);
        if (secondActionText != null) {
            Button second = (Button) view.findViewById(R.id.second_action);
            second.setText(secondActionText);
            second.setOnClickListener(getSecondActionListener());
            second.setVisibility(View.VISIBLE);
        }
        final View detailGroup = view.findViewById(R.id.detail_group);
        final ImageView expandIndicator = (ImageView) view.findViewById(R.id.expand_indicator);
        View.OnClickListener expandListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailGroup.getVisibility() == View.VISIBLE) {
                    expandIndicator.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_expand_more, context.getTheme()));
                    detailGroup.setVisibility(View.GONE);
                } else {
                    expandIndicator.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_expand_less, context.getTheme()));
                    detailGroup.setVisibility(View.VISIBLE);
                }
            }
        };
        view.findViewById(R.id.collapsed_group).setOnClickListener(expandListener);
        expandIndicator.setOnClickListener(expandListener);
        view.setTag(this);
        return view;
    }

    public Drawable getIcon(Context context) {
        if (iconRes != 0) {
            return ResourcesCompat.getDrawable(context.getResources(), iconRes, context.getTheme());
        }
        return icon;
    }

    public CharSequence getTitle(Context context) {
        if (titleRes != 0) {
            return context.getString(titleRes);
        }
        if (titlePluralsRes != 0) {
            return context.getResources().getQuantityString(titlePluralsRes, evaluatedPlurals);
        }
        return title;
    }

    public CharSequence getSummary(Context context) {
        if (summaryRes != 0) {
            return context.getString(summaryRes);
        }
        if (summaryPluralsRes != 0) {
            return context.getResources().getQuantityString(summaryPluralsRes, evaluatedPlurals);
        }
        return summary;
    }

    public View.OnClickListener getFirstActionListener() {
        return firstActionListener;
    }

    public CharSequence getFirstActionText(Context context) {
        if (firstActionTextRes != 0) {
            return context.getString(firstActionTextRes);
        }
        if (firstActionPluralsRes != 0) {
            return context.getResources().getQuantityString(firstActionPluralsRes, evaluatedPlurals);
        }
        return firstActionText;
    }

    public View.OnClickListener getSecondActionListener() {
        return secondActionListener;
    }

    public CharSequence getSecondActionText(Context context) {
        if (secondActionTextRes != 0) {
            return context.getString(secondActionTextRes);
        }
        if (secondActionPluralsRes != 0) {
            return context.getResources().getQuantityString(secondActionPluralsRes, evaluatedPlurals);
        }
        return secondActionText;
    }

    public synchronized boolean willBeEvaluating() {
        if (!evaluating && !evaluated && evaluation != null) {
            return evaluating = true;
        } else {
            return false;
        }
    }

    public boolean isEvaluated() {
        return evaluated || evaluation == null;
    }

    public synchronized void evaluate(Context context) {
        active = evaluation == null || evaluation.isActive(context);
        evaluatedPlurals = evaluation.getPluralsCount();
        evaluated = true;
        evaluating = false;
    }

    public boolean isActive(Context context) {
        if (!evaluated && evaluation != null) evaluate(context);
        return active;
    }

    public void resetEvaluated() {
        this.evaluated = false;
    }

    public static abstract class Evaluation {
        public abstract boolean isActive(Context context);

        public int getPluralsCount() {
            return 1;
        }
    }

    public static class Builder {

        @DrawableRes
        private int iconRes;
        private Drawable icon;
        @StringRes
        private int titleRes;
        @PluralsRes
        private int titlePluralsRes;
        private CharSequence title;
        @StringRes
        private int summaryRes;
        @PluralsRes
        private int summaryPluralsRes;
        private CharSequence summary;
        @StringRes
        private int firstActionTextRes;
        @PluralsRes
        private int firstActionPluralsRes;
        private CharSequence firstActionText;
        private View.OnClickListener firstActionListener;
        @StringRes
        private int secondActionTextRes;
        @PluralsRes
        private int secondActionPluralsRes;
        private CharSequence secondActionText;
        private View.OnClickListener secondActionListener;
        private Evaluation evaluation;


        public Builder() {
        }

        public Builder icon(Drawable val) {
            icon = val;
            return this;
        }

        public Builder icon(@DrawableRes int val) {
            iconRes = val;
            return this;
        }

        public Builder title(CharSequence val) {
            title = val;
            return this;
        }

        public Builder title(@StringRes int val) {
            titleRes = val;
            return this;
        }

        public Builder titlePlurals(@PluralsRes int val) {
            titlePluralsRes = val;
            return this;
        }

        public Builder summary(CharSequence val) {
            summary = val;
            return this;
        }

        public Builder summary(@StringRes int val) {
            summaryRes = val;
            return this;
        }

        public Builder summaryPlurals(@PluralsRes int val) {
            summaryPluralsRes = val;
            return this;
        }

        public Builder firstAction(CharSequence text, View.OnClickListener listener) {
            firstActionText = text;
            firstActionListener = listener;
            return this;
        }

        public Builder firstAction(@StringRes int val, View.OnClickListener listener) {
            firstActionTextRes = val;
            firstActionListener = listener;
            return this;
        }

        public Builder firstActionPlurals(@PluralsRes int val, View.OnClickListener listener) {
            firstActionPluralsRes = val;
            firstActionListener = listener;
            return this;
        }

        public Builder secondAction(CharSequence text, View.OnClickListener listener) {
            secondActionText = text;
            secondActionListener = listener;
            return this;
        }

        public Builder secondAction(@StringRes int val, View.OnClickListener listener) {
            secondActionTextRes = val;
            secondActionListener = listener;
            return this;
        }

        public Builder secondActionPlurals(@PluralsRes int val, View.OnClickListener listener) {
            secondActionPluralsRes = val;
            secondActionListener = listener;
            return this;
        }

        public Builder evaluation(Evaluation evaluation) {
            this.evaluation = evaluation;
            return this;
        }

        public Condition build() {
            return new Condition(this);
        }
    }
}
