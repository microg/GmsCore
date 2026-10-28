/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tflite;

import androidx.annotation.Keep;

@Keep
public final class TfLiteLogger {
    private volatile long timeoutInterpreterCreateMilliseconds = -1;
    private volatile long timeoutInterpreterInvokeMilliseconds = -1;

    public void setBuildCl(long buildCl) {
    }

    public void logEvent(int category, int event) {
    }

    public void logEventWithDuration(int category, int event, long duration) {
    }

    public void logCountersEvent(String name) {
    }

    public void logCountersTimer(String name, long duration) {
    }

    public void logCountersBooleanHistogram(String name, boolean value) {
    }

    public void logTfLiteSettings(byte[] settings) {
    }

    public void logTfLiteTelemetryEvent(byte[] event) {
    }

    public void logNnApiCompilationEvent(int category, int event, byte[] settings, byte[] metrics) {
    }

    public void logTimeoutEvent(int category, int event, byte[] settings) {
    }

    public void logNnApiExecutionCounters(byte[] settings, byte[] metrics, long first, long second, long third) {
    }

    public boolean getFlagNnapiSlEnableTelemetry() {
        return false;
    }

    public boolean getFlagTfliteEnableTelemetryProfiler() {
        return false;
    }

    public long getFlagTfliteTimeoutInterpreterCreateMilliseconds() {
        return timeoutInterpreterCreateMilliseconds;
    }

    public void overrideFlagTfliteTimeoutInterpreterCreateMillisecondsForTesting(long timeout) {
        timeoutInterpreterCreateMilliseconds = timeout;
    }

    public long getFlagTfliteTimeoutInterpreterInvokeMilliseconds() {
        return timeoutInterpreterInvokeMilliseconds;
    }

    public void overrideFlagTfliteTimeoutInterpreterInvokeMillisecondsForTesting(long timeout) {
        timeoutInterpreterInvokeMilliseconds = timeout;
    }
}
