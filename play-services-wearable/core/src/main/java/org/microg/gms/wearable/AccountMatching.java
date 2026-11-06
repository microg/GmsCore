package org.microg.gms.wearable;

import android.util.Log;

import org.microg.gms.wearable.proto.AccountMatchingMessage;
import org.microg.gms.wearable.proto.ControlMessage;
import org.microg.gms.wearable.proto.RootMessage;

import java.io.IOException;
import java.util.Collections;

public class AccountMatching {
    private static final String TAG = "AccountMatching";

    private static final int CONTROL_ACCOUNT_MATCHING = 6;

    private static final int GET_ACCOUNTS = 1;
    private static final int ACCOUNTS_RESPONSE = 2;
    private static final int REMOVE_ACCOUNTS = 3;
    private static final int REMOVE_ACCOUNTS_CONFIRM = 4;
    private static final int CANCEL = 5;

    private final WearableImpl wearable;

    public AccountMatching(WearableImpl wearable) {
        this.wearable = wearable;
    }

    public boolean handleControlMessage(WearableConnection connection, String sourceNodeId, ControlMessage control) {
        if (control == null) {
            return false;
        }

        if (control.type == null || control.type != CONTROL_ACCOUNT_MATCHING) {
            return false;
        }

        AccountMatchingMessage msg = control.accountMatching;

        if (msg == null) {
            Log.w(TAG, "ACCOUNT_MATCHING control message with null body");
            return true;
        }

        int type = msg.type != null ? msg.type : 0;

        Log.d(TAG, "[" + sourceNodeId + "] account matching type=" + type);

        switch (type) {
            case GET_ACCOUNTS:
                sendAccountsResponse(connection, sourceNodeId);
                break;

            case REMOVE_ACCOUNTS:
                sendRemoveAccountsConfirm(connection, sourceNodeId);
                break;

            case CANCEL:
                break;

            case ACCOUNTS_RESPONSE:
                break;

            default:
                break;
        }

        return true;
    }

    private void sendAccountsResponse(WearableConnection connection, String targetNodeId) {
        try {
            AccountMatchingMessage reply = new AccountMatchingMessage.Builder()
                    .type(ACCOUNTS_RESPONSE)
                    .entries(Collections.emptyList())
                    .build();

            connection.writeMessage(buildControlMessage(targetNodeId, reply));
            Log.d(TAG, "Sent ACCOUNTS_RESPONSE (empty) for " + targetNodeId);

        } catch (IOException e) {
            Log.e(TAG, "Failed to send ACCOUNTS_RESPONSE for " + targetNodeId, e);
        }
    }

    private void sendRemoveAccountsConfirm(WearableConnection connection, String targetNodeId) {
        try {
            AccountMatchingMessage reply = new AccountMatchingMessage.Builder()
                    .type(REMOVE_ACCOUNTS_CONFIRM)
                    .build();

            connection.writeMessage(buildControlMessage(targetNodeId, reply));
            Log.d(TAG, "Sent REMOVE_ACCOUNTS_CONFIRM for "+ targetNodeId);

        } catch (IOException e) {
            Log.e(TAG, "Failed to send REMOVE_ACCOUNTS_CONFIRM for " + targetNodeId, e);
        }
    }

    private RootMessage buildControlMessage(String targetNodeId,
                                            AccountMatchingMessage msg) {
        ControlMessage ctrl = new ControlMessage.Builder()
                .type(CONTROL_ACCOUNT_MATCHING)
                .accountMatching(msg)
                .build();

        return new RootMessage.Builder()
                .controlMessage(ctrl)
                .build();
    }
}
