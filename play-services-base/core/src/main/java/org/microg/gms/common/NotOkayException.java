package org.microg.gms.common;

import java.io.IOException;

public class NotOkayException extends IOException {
    public NotOkayException() {
    }

    public NotOkayException(String message) {
        super(message);
    }

    public NotOkayException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotOkayException(Throwable cause) {
        super(cause);
    }
}
