/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.android.gms.wearable.internal.CapabilityInfoParcelable;
import com.google.android.gms.wearable.internal.IWearableListener;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded capability delivery. Neither the network looper nor Android's main thread performs IPC. */
final class CapabilityListenerDispatcher {
    private static final int MAX_PENDING = 32;
    private static final int MAX_PER_PACKAGE = 8;
    private static final long TIMEOUT_MS = 10_000;
    private static final ProcessBudget BUDGET = new ProcessBudget();
    private static final ThreadPoolExecutor WORKERS = workers();
    private final Context context;
    private final WearableImpl wearable;
    private final Set<Delivery> pending = new HashSet<>();
    private final Map<String, Event> latest = new HashMap<>();
    private final Map<String, ArrayDeque<Work>> queued = new HashMap<>();
    private final Set<String> running = new HashSet<>();
    private volatile boolean stopped;

    private static ThreadPoolExecutor workers() {
        ThreadPoolExecutor workers = new ThreadPoolExecutor(MAX_PENDING, MAX_PENDING,
                60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(MAX_PENDING), task -> {
                Thread thread = new Thread(task, "wearable-capability-listener");
                thread.setDaemon(true);
                return thread;
            });
        workers.allowCoreThreadTimeOut(true);
        return workers;
    }

    CapabilityListenerDispatcher(Context context, WearableImpl wearable) {
        this.context = context;
        this.wearable = wearable;
    }

    void send(String path, String signature, Intent intent, CapabilityInfoParcelable info,
              List<IWearableListener> targets) {
        Event event = new Event(path, signature, intent, info);
        List<Delivery> deliveries = new ArrayList<>();
        List<Delivery> superseded = new ArrayList<>();
        synchronized (this) {
            if (stopped) return;
            Event previous = latest.remove(path);
            if (previous != null) {
                previous.current = false;
                for (Delivery delivery : pending) {
                    if (delivery.event == previous) superseded.add(delivery);
                }
            }
        }
        // Retire queued/binding work immediately; a synchronous call already in flight keeps its quota.
        for (Delivery delivery : superseded) delivery.finish();
        synchronized (this) {
            if (stopped) return;
            for (IWearableListener target : targets) {
                Delivery delivery = admit(event, target);
                if (delivery == null) break;
                deliveries.add(delivery);
            }
            Delivery manifest = admit(event, null);
            if (manifest != null) deliveries.add(manifest);
            if (!deliveries.isEmpty()) latest.put(path, event);
        }
        for (Delivery delivery : deliveries) delivery.start();
    }

    private Delivery admit(Event event, IWearableListener target) {
        if (!BUDGET.acquire(event.intent.getPackage())) return null;
        Delivery delivery = new Delivery(event, target);
        pending.add(delivery);
        event.pending++;
        return delivery;
    }

    private synchronized void release(Delivery delivery) {
        if (!pending.remove(delivery)) return;
        BUDGET.release(delivery.event.intent.getPackage());
        ArrayDeque<Work> queue = queued.get(delivery.event.intent.getPackage());
        if (queue != null) queue.removeIf(work -> work.delivery == delivery);
        Event event = delivery.event;
        if (--event.pending == 0 && latest.get(event.path) == event) latest.remove(event.path);
    }

    private void enqueue(Delivery delivery, Runnable action) {
        String packageName = delivery.event.intent.getPackage();
        boolean start;
        synchronized (this) {
            if (!pending.contains(delivery) || stopped) return;
            queued.computeIfAbsent(packageName, key -> new ArrayDeque<>()).add(new Work(delivery, action));
            start = running.add(packageName);
        }
        if (start) {
            try { WORKERS.execute(() -> drain(packageName)); }
            catch (RejectedExecutionException e) {
                List<Work> rejected;
                synchronized (this) {
                    ArrayDeque<Work> queue = queued.remove(packageName);
                    rejected = queue == null ? new ArrayList<>() : new ArrayList<>(queue);
                    running.remove(packageName);
                }
                for (Work work : rejected) work.delivery.finish();
            }
        }
    }

    private void drain(String packageName) {
        while (true) {
            Work work;
            synchronized (this) {
                ArrayDeque<Work> queue = queued.get(packageName);
                work = queue == null ? null : queue.poll();
                if (work == null) {
                    queued.remove(packageName);
                    running.remove(packageName);
                    return;
                }
            }
            work.delivery.run(work.action);
        }
    }

    private final class Work {
        final Delivery delivery;
        final Runnable action;
        Work(Delivery delivery, Runnable action) { this.delivery = delivery; this.action = action; }
    }

    void stop() {
        List<Delivery> deliveries;
        synchronized (this) {
            stopped = true;
            for (Event event : latest.values()) event.current = false;
            latest.clear();
            queued.clear();
            running.clear();
            deliveries = new ArrayList<>(pending);
        }
        for (Delivery delivery : deliveries) delivery.finish();
        // The pool and budget belong to the process. A blocked Binder from this instance must
        // still consume its quota if Android recreates WearableService in the same process.
    }

    private static final class ProcessBudget {
        private final Map<String, Integer> packages = new HashMap<>();
        private int total;
        synchronized boolean acquire(String packageName) {
            int count = packages.containsKey(packageName) ? packages.get(packageName) : 0;
            if (total >= MAX_PENDING || count >= MAX_PER_PACKAGE) return false;
            packages.put(packageName, count + 1);
            total++;
            return true;
        }
        synchronized void release(String packageName) {
            int remaining = packages.get(packageName) - 1;
            if (remaining == 0) packages.remove(packageName);
            else packages.put(packageName, remaining);
            total--;
        }
    }

    private static final class Event {
        final String path;
        final String signature;
        final Intent intent;
        final CapabilityInfoParcelable info;
        volatile boolean current = true;
        int pending;
        Event(String path, String signature, Intent intent, CapabilityInfoParcelable info) {
            this.path = path;
            this.signature = signature;
            this.intent = intent;
            this.info = info;
        }
    }

    private final class Delivery implements ServiceConnection {
        final Event event;
        final IWearableListener registered;
        final AtomicBoolean connected = new AtomicBoolean();
        final Runnable timeout = this::finish;
        boolean bound;
        boolean finished;
        int working;

        Delivery(Event event, IWearableListener registered) {
            this.event = event;
            this.registered = registered;
        }

        boolean allowed() {
            return !stopped && event.current
                    && wearable.matchesInstalledSignature(event.intent.getPackage(), event.signature);
        }

        void start() {
            if (!wearable.networkHandler.postDelayed(timeout, TIMEOUT_MS)) { finish(); return; }
            // Publication can race stop(), which must not leave an orphan timer behind.
            synchronized (this) {
                if (finished) { wearable.networkHandler.removeCallbacks(timeout); return; }
            }
            dispatch(registered == null ? this::bind : () -> invoke(registered));
        }

        void dispatch(Runnable action) {
            enqueue(this, action);
        }

        void run(Runnable action) {
            synchronized (this) {
                if (finished) return;
                working++;
            }
            try { action.run(); }
            catch (RuntimeException ignored) { finish(); }
            finally {
                boolean release;
                synchronized (this) { release = --working == 0 && finished; }
                if (release) CapabilityListenerDispatcher.this.release(this);
            }
        }

        void bind() {
            if (!allowed()) { finish(); return; }
            ResolveInfo resolved = context.getPackageManager().resolveService(event.intent, 0);
            if (resolved == null || resolved.serviceInfo == null
                    || !event.intent.getPackage().equals(resolved.serviceInfo.packageName)) {
                finish();
                return;
            }
            Intent bind = new Intent("com.google.android.gms.wearable.BIND_LISTENER")
                    .setClassName(resolved.serviceInfo.packageName, resolved.serviceInfo.name);
            if (!allowed()) { finish(); return; }
            boolean attached = context.bindService(bind, this, Context.BIND_AUTO_CREATE);
            boolean close;
            synchronized (this) {
                close = attached && finished;
                if (attached && !finished) bound = true;
            }
            if (close) unbind();
            if (!attached) finish();
        }

        void invoke(IWearableListener listener) {
            try {
                // Work is serialized per application, so removal cannot be overtaken by an older IPC.
                synchronized (this) { if (finished) return; }
                if (allowed()) listener.onConnectedCapabilityChanged(event.info);
            } catch (RemoteException ignored) {
                if (registered != null) wearable.removeListener(registered);
            } finally { finish(); }
        }

        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            if (!event.intent.getPackage().equals(name.getPackageName()) || binder == null) { finish(); return; }
            if (connected.compareAndSet(false, true)) dispatch(() -> invoke(IWearableListener.Stub.asInterface(binder)));
        }
        @Override public void onServiceDisconnected(ComponentName name) { finish(); }
        @Override public void onBindingDied(ComponentName name) { finish(); }
        @Override public void onNullBinding(ComponentName name) { finish(); }

        void finish() {
            boolean detach;
            boolean release;
            synchronized (this) {
                if (finished) return;
                finished = true;
                detach = bound;
                bound = false;
                // An unresponsive Binder retains its quota until the worker actually returns.
                release = working == 0;
            }
            wearable.networkHandler.removeCallbacks(timeout);
            if (detach) unbind();
            if (release) CapabilityListenerDispatcher.this.release(this);
        }

        void unbind() {
            try { context.unbindService(this); }
            catch (IllegalArgumentException | SecurityException ignored) { }
        }
    }
}
