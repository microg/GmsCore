/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import android.os.ParcelFileDescriptor;
import org.microg.gms.common.PublicApi;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

@PublicApi
public class Payload {
    public static final class Type {
        public static final int BYTES = 1;
        public static final int FILE = 2;
        public static final int STREAM = 3;

        private Type() {}
    }

    private final long id;
    private final int type;
    private final byte[] bytes;
    private final File file;
    private final Stream stream;

    private Payload(long id, int type, byte[] bytes, File file, Stream stream) {
        this.id = id;
        this.type = type;
        this.bytes = bytes;
        this.file = file;
        this.stream = stream;
    }

    public static Payload fromBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Cannot create a Payload from null bytes.");
        }
        return new Payload(UUID.randomUUID().getLeastSignificantBits(), Type.BYTES, bytes, null, null);
    }

    public static Payload fromFile(java.io.File file) throws FileNotFoundException {
        return fromFile(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
    }

    public static Payload fromFile(ParcelFileDescriptor pfd) {
        if (pfd == null) {
            throw new IllegalArgumentException("Cannot create a Payload from a null ParcelFileDescriptor.");
        }
        return new Payload(UUID.randomUUID().getLeastSignificantBits(), Type.FILE, null, new File(pfd), null);
    }

    public static Payload fromStream(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Cannot create a Payload from a null InputStream.");
        }
        return new Payload(UUID.randomUUID().getLeastSignificantBits(), Type.STREAM, null, null, new Stream(inputStream));
    }

    public static Payload fromStream(ParcelFileDescriptor pfd) {
        if (pfd == null) {
            throw new IllegalArgumentException("Cannot create a Payload from a null ParcelFileDescriptor.");
        }
        return new Payload(UUID.randomUUID().getLeastSignificantBits(), Type.STREAM, null, null, new Stream(pfd));
    }

    public long getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public byte[] asBytes() {
        return bytes;
    }

    public File asFile() {
        return file;
    }

    public Stream asStream() {
        return stream;
    }

    @PublicApi
    public static class File {
        private final ParcelFileDescriptor pfd;
        private final long size;

        private File(ParcelFileDescriptor pfd) {
            this.pfd = pfd;
            this.size = pfd != null ? pfd.getStatSize() : -1;
        }

        public ParcelFileDescriptor asParcelFileDescriptor() {
            return pfd;
        }

        public java.io.File asJavaFile() {
            return null;
        }

        public long getSize() {
            return size;
        }
    }

    @PublicApi
    public static class Stream {
        private final InputStream inputStream;
        private final ParcelFileDescriptor pfd;

        private Stream(InputStream inputStream) {
            this.inputStream = inputStream;
            this.pfd = null;
        }

        private Stream(ParcelFileDescriptor pfd) {
            this.inputStream = pfd != null ? new ParcelFileDescriptor.AutoCloseInputStream(pfd) : null;
            this.pfd = pfd;
        }

        public InputStream asInputStream() {
            return inputStream;
        }

        public ParcelFileDescriptor asParcelFileDescriptor() {
            return pfd;
        }
    }
}
