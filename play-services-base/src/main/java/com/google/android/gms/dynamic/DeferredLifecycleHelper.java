/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.dynamic;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;

public abstract class DeferredLifecycleHelper<T extends LifecycleDelegate> {
    private T delegate;
    private Bundle savedInstanceState;
    private final LinkedList<PendingStateOperation<T>> pendingStateOperations = new LinkedList<>();
    private final OnDelegateCreatedListener<T> listener = (delegate) -> {
        DeferredLifecycleHelper.this.delegate = delegate;
        for (PendingStateOperation<T> op : pendingStateOperations) {
            op.apply(delegate);
        }
        pendingStateOperations.clear();
        savedInstanceState = null;
    };

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        FrameLayout rootLayout = new FrameLayout(inflater.getContext());
        startStateOperation(savedInstanceState, new PendingStateOperation<T>(State.VIEW_CREATED) {
            @Override
            public void apply(T delegate) {
                rootLayout.removeAllViews();
                rootLayout.addView(delegate.onCreateView(inflater, parent, savedInstanceState));
            }
        });
        return rootLayout;
    }

    public T getDelegate() {
        return delegate;
    }

    protected abstract void createDelegate(@NonNull OnDelegateCreatedListener<T> listener);

    public void onCreate(@Nullable Bundle savedInstanceState) {
        startStateOperation(savedInstanceState, new PendingStateOperation<T>(State.CREATED) {
            @Override
            public void apply(T delegate) {
                delegate.onCreate(savedInstanceState);
            }
        });
    }

    public void onDestroy() {
        if (delegate != null) {
            delegate.onDestroy();
        } else {
            removePendingStateOperations(State.CREATED);
        }
    }

    public void onDestroyView() {
        if (delegate != null) {
            delegate.onDestroyView();
        } else {
            removePendingStateOperations(State.VIEW_CREATED);
        }
    }

    public void onInflate(@NonNull Activity activity, @NonNull Bundle attrs, @Nullable Bundle savedInstanceState) {
        startStateOperation(savedInstanceState, new PendingStateOperation<T>(State.NONE) {
            @Override
            public void apply(T delegate) {
                delegate.onInflate(activity, attrs, savedInstanceState);
            }
        });
    }

    public void onLowMemory() {
        if (delegate != null) delegate.onLowMemory();
    }

    public void onPause() {
        if (delegate != null) {
            delegate.onPause();
        } else {
            removePendingStateOperations(State.RESUMED);
        }
    }

    public void onResume() {
        startStateOperation(savedInstanceState, new PendingStateOperation<T>(State.RESUMED) {
            @Override
            public void apply(T delegate) {
                delegate.onResume();
            }
        });
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (delegate != null) {
            delegate.onSaveInstanceState(outState);
        } else if (savedInstanceState != null) {
            outState.putAll(savedInstanceState);
        }
    }

    public void onStart() {
        startStateOperation(savedInstanceState, new PendingStateOperation<T>(State.STARTED) {
            @Override
            public void apply(T delegate) {
                delegate.onStart();
            }
        });
    }

    public void onStop() {
        if (delegate != null) {
            delegate.onStop();
        } else {
            removePendingStateOperations(State.STARTED);
        }
    }

    private void removePendingStateOperations(State state) {
        while (!pendingStateOperations.isEmpty() && pendingStateOperations.getLast().state.isAtLeast(state)) {
            pendingStateOperations.removeLast();
        }
    }

    private void startStateOperation(@Nullable Bundle savedInstanceState, PendingStateOperation<T> op) {
        if (delegate != null) {
            op.apply(delegate);
        } else {
            pendingStateOperations.add(op);
            if (savedInstanceState != null) {
                if (this.savedInstanceState == null) this.savedInstanceState = new Bundle();
                this.savedInstanceState.putAll(savedInstanceState);
            }
            createDelegate(listener);
        }
    }

    private static abstract class PendingStateOperation<T extends LifecycleDelegate> {
        public final State state;

        public PendingStateOperation(State state) {
            this.state = state;
        }

        public abstract void apply(T delegate);
    }

    private enum State {
        NONE, CREATED, VIEW_CREATED, STARTED, RESUMED;

        public boolean isAtLeast(@NonNull State state) {
            return compareTo(state) >= 0;
        }
    }
}
