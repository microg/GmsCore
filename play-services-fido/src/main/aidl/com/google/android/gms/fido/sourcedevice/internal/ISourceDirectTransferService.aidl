package com.google.android.gms.fido.sourcedevice.internal;

import android.os.ParcelFileDescriptor;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.fido.sourcedevice.SourceStartDirectTransferOptions;
import com.google.android.gms.fido.sourcedevice.internal.IStartDirectTransferCallbacks;

interface ISourceDirectTransferService {
    void startDirectTransfer(
        IStartDirectTransferCallbacks callbacks,
        in SourceStartDirectTransferOptions options,
        in ParcelFileDescriptor input,
        in ParcelFileDescriptor output
    );

    void startDirectTransferV2(
        IStatusCallback callback,
        in SourceStartDirectTransferOptions options,
        in ParcelFileDescriptor input,
        in ParcelFileDescriptor output
    );
}
