package org.microg.gms.wearable.channel;

public class ChannelStatusCodes {
    public static final int SUCCESS = 0;
    public static final int CLOSE_REASON_NORMAL = 0;
    public static final int CLOSE_REASON_LOCAL_CLOSE = 1;
    public static final int CLOSE_REASON_REMOTE_CLOSE = 3;
    public static final int INTERNAL_ERROR = 8;
    public static final int CHANNEL_NOT_CONNECTED = 13;
    public static final int CHANNEL_CLOSED = 16;

    // Custom codes
    public static final int INVALID_ARGUMENT = 10003;
    public static final int CHANNEL_NOT_FOUND = 10004;
    public static final int ALREADY_IN_PROGRESS = 10005;
    public static final int CHANNEL_LIMIT_REACHED = 10006;
    public static final int INVALID_PACKAGE = 10007;
    public static final int INVALID_PATH = 10008;

    public static String getStatusName(int status) {
        switch (status) {
            case SUCCESS: return "SUCCESS";
            case CLOSE_REASON_LOCAL_CLOSE: return "LOCAL_CLOSE";
            case CLOSE_REASON_REMOTE_CLOSE: return "REMOTE_CLOSE";
            case INTERNAL_ERROR: return "INTERNAL_ERROR";
            case CHANNEL_NOT_CONNECTED: return "NOT_CONNECTED";
            case CHANNEL_CLOSED: return "CLOSED";
            case INVALID_ARGUMENT: return "INVALID_ARGUMENT";
            case CHANNEL_NOT_FOUND: return "NOT_FOUND";
            case ALREADY_IN_PROGRESS: return "ALREADY_IN_PROGRESS";
            case CHANNEL_LIMIT_REACHED: return "LIMIT_REACHED";
            case INVALID_PACKAGE: return "INVALID_PACKAGE";
            case INVALID_PATH: return "INVALID_PATH";
            default: return "UNKNOWN(" + status + ")";
        }
    }
}