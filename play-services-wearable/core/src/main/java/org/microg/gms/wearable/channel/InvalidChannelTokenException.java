package org.microg.gms.wearable.channel;

public class InvalidChannelTokenException extends Exception {
    public InvalidChannelTokenException() { super(); }
    public InvalidChannelTokenException(String message) { super(message); }
    public InvalidChannelTokenException(String message, Throwable cause) { super(message, cause); }
}