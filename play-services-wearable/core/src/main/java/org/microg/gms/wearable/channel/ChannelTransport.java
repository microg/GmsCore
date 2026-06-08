package org.microg.gms.wearable.channel;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChannelTransport {
    private static final String TAG = "ChannelTransport";

    private final Map<ParcelFileDescriptor, IOMode> fdModes = new ConcurrentHashMap<>();
//    private final Map<ParcelFileDescriptor, FileInputStream> inputStreams = new ConcurrentHashMap<>();
    private final Map<ParcelFileDescriptor, FdReader> fdReaders = new ConcurrentHashMap<>();
    private final Map<ParcelFileDescriptor, FileOutputStream> outputStreams = new ConcurrentHashMap<>();

    private static final byte[] EOF_SENTINEL = new byte[0];

    private static class FdReader {
        final LinkedBlockingDeque<byte[]> queue = new LinkedBlockingDeque<>();
        final AtomicBoolean eof = new AtomicBoolean(false);
        final Thread thread;

        FdReader(final FileInputStream fis, final ParcelFileDescriptor fd) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = new byte[65536];
                    try {
                        int n;
                        while ((n = fis.read(buf)) != -1) {
                            if (n > 0) {
                                byte[] chunk = new byte[n];
                                System.arraycopy(buf, 0, chunk, 0, n);
                                queue.put(chunk);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (IOException ignored) {
                    } finally {
                        eof.set(true);
                        try {
                            queue.put(EOF_SENTINEL);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }, "ChannelReader-" + fd);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public enum IOMode {
        NONE,
        READ,
        WRITE
    }

    public void register(ParcelFileDescriptor fd) {
        if (fd != null) {
            fdModes.put(fd, IOMode.NONE);

//            inputStreams.put(fd, new FileInputStream(fd.getFileDescriptor()));
            fdReaders.put(fd, new FdReader(new FileInputStream(fd.getFileDescriptor()), fd));
            outputStreams.put(fd, new FileOutputStream(fd.getFileDescriptor()));
            Log.d(TAG, "Registered FD: " + fd);
        }
    }

    public void unregister(ParcelFileDescriptor fd) {
        if (fd != null) {
            fdModes.remove(fd);
//            FileInputStream fis = inputStreams.remove(fd);
//            if (fis != null) {
//                try {
//                    fis.close();
//                } catch (IOException ignored) {}
//            }
            FdReader reader = fdReaders.remove(fd);
            if (reader != null)
                reader.thread.interrupt();
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

        if (len == 0)
            return 0;

        FdReader reader = fdReaders.get(fd);
        if (reader == null)
            throw new IOException("No reader for FD");

        byte[] chunk = reader.queue.peek();
        if (chunk == null)
            return reader.eof.get() ? -1 : 0;

        if (chunk == EOF_SENTINEL)
            return -1;

        chunk = reader.queue.poll();
        if (chunk == null || chunk == EOF_SENTINEL)
            return -1;
        
        int n = Math.min(len, chunk.length);
        System.arraycopy(chunk, 0, buf, offset, n);
        
        if (n < chunk.length) {
            byte[] remainder = new byte[chunk.length - n];
            System.arraycopy(chunk, n, remainder, 0, remainder.length);
            reader.queue.offerFirst(remainder);
        }

        Log.d(TAG, "read: Read " + n + " bytes from FD");
        return n;
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
//        inputStreams.clear();
        for (FdReader reader : fdReaders.values()) {
            reader.thread.interrupt();
        }
        fdReaders.clear();
        outputStreams.clear();
    }
}