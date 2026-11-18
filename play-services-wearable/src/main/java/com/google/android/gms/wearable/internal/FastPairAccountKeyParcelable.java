package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.nio.ByteBuffer;

public class FastPairAccountKeyParcelable extends AutoSafeParcelable {
    @SafeParceled(1)
    public byte[] accountKey;

    private FastPairAccountKeyParcelable() {}

    public FastPairAccountKeyParcelable(byte[] accountKey) {
        this.accountKey = accountKey;
    }

    public FastPairAccountKeyParcelable(ByteBuffer buffer) {
        ByteBuffer copy = buffer.duplicate();
        this.accountKey = new byte[copy.remaining()];
        copy.get(this.accountKey);
    }

    public static final Creator<FastPairAccountKeyParcelable> CREATOR =
            new AutoCreator<FastPairAccountKeyParcelable>(FastPairAccountKeyParcelable.class);
}