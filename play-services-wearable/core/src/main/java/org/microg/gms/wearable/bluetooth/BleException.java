package org.microg.gms.wearable.bluetooth;

public class BleException extends Exception {
    public static final int CODE_UNKNOWN = -1;
    public static final int CODE_GATT_INVALID_HANDLE = 1;
    public static final int CODE_GATT_READ_NOT_PERMITTED = 2;
    public static final int CODE_GATT_WRITE_NOT_PERMITTED = 3;

    public static final int CODE_TIME_SERVICE_NOT_FOUND = 256;
    public static final int CODE_MISSING_CLOCKWORK_CHARS = 258;
    public static final int CODE_INVALID_DECOMMISSION = 259;
    public static final int CODE_SERVICE_NOT_FOUND = 260;
    public static final int CODE_TIME_CHAR_INVALID = 261;
    public static final int CODE_TIMEZONE_OFFSET_INVALID = 262;

    public final int statusCode;

    public BleException(String message) {
        super(message);
        this.statusCode = CODE_UNKNOWN;
    }

    public BleException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public BleException(Throwable cause) {
        super(cause);
        this.statusCode = CODE_UNKNOWN;
    }
}
