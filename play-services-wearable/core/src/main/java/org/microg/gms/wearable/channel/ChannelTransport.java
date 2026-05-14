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
    private final Map<ParcelFileDescriptor, FileInputStream> inputStreams = new ConcurrentHashMap<>();
    private final Map<ParcelFileDescriptor, FileOutputStream> outputStreams = new ConcurrentHashMap<>();


    public enum IOMode {
        NONE,
        READ,
        WRITE
    }

    public void register(ParcelFileDescriptor fd) {
        if (fd != null) {
            fdModes.put(fd, IOMode.NONE);
            inputStreams.put(fd, new FileInputStream(fd.getFileDescriptor()));
            outputStreams.put(fd, new FileOutputStream(fd.getFileDescriptor()));
            Log.d(TAG, "Registered FD: " + fd);
        }
    }

    public void unregister(ParcelFileDescriptor fd) {
        if (fd != null) {
            fdModes.remove(fd);
            FileInputStream fis = inputStreams.remove(fd);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {}
            }
            FileOutputStream fos = outputStreams.remove(fd);
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {}
            }
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
            FileInputStream fis = inputStreams.get(fd);
            if (fis == null) throw new IOException("No cached stream for FD");
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
            FileOutputStream fos = outputStreams.get(fd);
            if (fos == null) throw new IOException("No cached stream for FD");
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
        inputStreams.clear();
        outputStreams.clear();
    }
}