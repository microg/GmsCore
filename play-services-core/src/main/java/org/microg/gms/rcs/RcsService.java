/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.os.RemoteException;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Diagnostic entry point for the RCS-related GMS services (issue #2994).
 *
 * <p>Google Messages connects to RCS through four Play Services entry points:
 * {@code RCS}, {@code CARRIER_AUTH}, {@code CONSTELLATION} and {@code ASTERISM}. Until
 * recently all four were answered by {@code DummyService} with {@code API_DISABLED}, so
 * the RCS setup flow in Messages died immediately and left no trace of what it actually
 * tried to call.
 *
 * <p>This service changes only that: binding now succeeds and returns a
 * {@link RcsTraceBinder} that records which methods Messages attempts to call, so the
 * real interfaces can be identified from a device log before they are implemented. It
 * does <em>not</em> implement RCS, phone-number verification, carrier auth, or
 * provisioning, and it must not be presented as doing so. Each of those is a later
 * phase that will split out of this class into its own service.
 *
 * <p>Privacy: only bind metadata (caller package, service, transaction code, announced
 * interface descriptor, parcel size) is recorded. Parcel contents are never logged or
 * stored. See {@link RcsTrace}.
 */
public class RcsService extends BaseService {
    private final RcsTrace trace = new RcsTrace();

    public RcsService() {
        super("GmsRcsSvc", GmsService.RCS, GmsService.CARRIER_AUTH, GmsService.CONSTELLATION, GmsService.ASTERISM);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String caller = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        trace.record(service.name(), caller, 0, "bind", 0);
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS,
                new RcsTraceBinder(trace, service.name(), caller), null);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(fd, writer, args);
        writer.println("Recent RCS binder traces (newest last):");
        for (RcsTrace.Entry entry : trace.snapshot()) {
            writer.println("  " + entry);
        }
    }
}
