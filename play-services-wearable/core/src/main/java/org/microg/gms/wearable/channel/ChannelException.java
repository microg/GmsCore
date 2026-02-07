package org.microg.gms.wearable.channel;

public class ChannelException extends Exception {
    private final ChannelToken token;

    public ChannelException(ChannelToken token) {
        super("Channel exception for: " + token);
        this.token = token;
    }

    public ChannelException(ChannelToken token, String message) {
        super(message);
        this.token = token;
    }

    public ChannelException(ChannelToken token, Throwable cause) {
        super("Channel exception for: " + token, cause);
        this.token = token;
    }

    public ChannelToken getToken() {
        return token;
    }
}