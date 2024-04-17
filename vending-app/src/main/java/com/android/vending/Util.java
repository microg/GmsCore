package com.android.vending;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class Util {

    private static final String TAG = "FakeStoreUtil";

    /**
     * From <a href="https://stackoverflow.com/a/46688434/">StackOverflow</a>, CC BY-SA 4.0 by Sergey Frolov, adapted.
     */
    public static byte[] encodeGzip(final byte[] input) {

        try (final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
             final GZIPOutputStream gzipOutput = new GZIPOutputStream(byteOutput)) {
            gzipOutput.write(input);
            gzipOutput.finish();
            return byteOutput.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Failed to encode bytes as GZIP");
            return new byte[0];
        }
    }
}
