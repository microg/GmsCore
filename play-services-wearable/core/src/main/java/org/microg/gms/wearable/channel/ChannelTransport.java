package org.microg.gms.wearable.channel;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelTransport {
    private static final String TAG = "ChannelTransport";

    private final Map<ParcelFileDescriptor, IOMode> fdModes = new ConcurrentHashMap<>();

    public enum IOMode {
        NONE,
        READ,
        WRITE
    }

    public void register(ParcelFileDescriptor fd) {
        if (fd != null) {
            fdModes.put(fd, IOMode.NONE);
            Log.d(TAG, "Registered FD: " + fd);
        }
    }

    public void unregister(ParcelFileDescriptor fd) {
        if (fd != null) {
            fdModes.remove(fd);
            Log.d(TAG, "Unregistered FD: " + fd);
        }
    }

    public void setMode(ParcelFileDescriptor fd, IOMode mode) {
        if (fd != null) {
            fdModes.put(fd, mode);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Set FD mode to " + mode + ": " + fd);
            }
        }
    }

    public int read(ParcelFileDescriptor fd, byte[] buf, int offset, int len) throws IOException {
        if (fd == null || !fd.getFileDescriptor().valid()) {
            throw new IOException("Invalid file descriptor");
        }

        try {
            FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
            int bytesRead = fis.read(buf, offset, len);

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Read " + bytesRead + " bytes from FD");
            }

            return bytesRead;

        } catch (EOFException e) {
            return -1;
        }
    }

    public int write(ParcelFileDescriptor fd, byte[] buf, int offset, int len) throws IOException {
        if (fd == null || !fd.getFileDescriptor().valid()) {
            throw new IOException("Invalid file descriptor");
        }

        try {
            FileOutputStream fos = new FileOutputStream(fd.getFileDescriptor());
            fos.write(buf, offset, len);
            fos.flush();

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Wrote " + len + " bytes to FD");
            }

            return len;

        } catch (EOFException e) {
            return -1;
        }
    }

    public void clear() {
        fdModes.clear();
    }
}