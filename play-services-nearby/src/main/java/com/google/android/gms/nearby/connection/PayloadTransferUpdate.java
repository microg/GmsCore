/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import org.microg.gms.common.PublicApi;

import java.util.Objects;

@PublicApi
public final class PayloadTransferUpdate {
    public static final class Status {
        public static final int SUCCESS = 1;
        public static final int FAILURE = 2;
        public static final int IN_PROGRESS = 3;
        public static final int CANCELED = 4;

        private Status() {}
    }

    private final long payloadId;
    private final int status;
    private final long totalBytes;
    private final long bytesTransferred;

    public PayloadTransferUpdate(long payloadId, int status, long totalBytes, long bytesTransferred) {
        this.payloadId = payloadId;
        this.status = status;
        this.totalBytes = totalBytes;
        this.bytesTransferred = bytesTransferred;
    }

    public long getPayloadId() {
        return payloadId;
    }

    public int getStatus() {
        return status;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PayloadTransferUpdate that = (PayloadTransferUpdate) o;
        return payloadId == that.payloadId &&
                status == that.status &&
                totalBytes == that.totalBytes &&
                bytesTransferred == that.bytesTransferred;
    }

    @Override
    public int hashCode() {
        return Objects.hash(payloadId, status, totalBytes, bytesTransferred);
    }

    @PublicApi
    public static final class Builder {
        private long payloadId;
        private int status;
        private long totalBytes;
        private long bytesTransferred;

        public Builder() {}

        public Builder(PayloadTransferUpdate update) {
            if (update != null) {
                this.payloadId = update.payloadId;
                this.status = update.status;
                this.totalBytes = update.totalBytes;
                this.bytesTransferred = update.bytesTransferred;
            }
        }

        public Builder setPayloadId(long payloadId) {
            this.payloadId = payloadId;
            return this;
        }

        public Builder setStatus(int status) {
            this.status = status;
            return this;
        }

        public Builder setTotalBytes(long totalBytes) {
            this.totalBytes = totalBytes;
            return this;
        }

        public Builder setBytesTransferred(long bytesTransferred) {
            this.bytesTransferred = bytesTransferred;
            return this;
        }

        public PayloadTransferUpdate build() {
            return new PayloadTransferUpdate(payloadId, status, totalBytes, bytesTransferred);
        }
    }
}
