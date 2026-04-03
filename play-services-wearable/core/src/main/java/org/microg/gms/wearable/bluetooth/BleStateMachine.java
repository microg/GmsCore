package org.microg.gms.wearable.bluetooth;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BleStateMachine extends Handler {
    private final String name;
    private final Map<BleState, List<BleState>> transitions = new HashMap<>();
    private BleState currentState;
    private BleState errorState;

    protected BleStateMachine(String name, Looper looper) {
        super(looper);
        this.name = name;
    }

    protected void addState(BleState state) {
        transitions.put(state, new ArrayList<>());
    }

    protected void addTransition(BleState from, BleState to) {
        List<BleState> targets = transitions.get(from);

        if (targets == null) {
            Log.w(name, "addTransition: unknown source state " + from.getName());
            return;
        }

        if (!targets.contains(to)) {
            targets.add(to);
        }
    }

    protected void setErrorState(BleState state){
        this.errorState = state;
    }

    protected void start() {

    }

    public void transitionTo(BleState next) {
        if (currentState != null) {
            currentState.onExit();
        }
        currentState = next;
        if (currentState != null) {
            currentState.onEnter();
        }
    }

    public BleState currentState() {
        return currentState;
    }

    public void sendMessage(int what) {
        sendMessage(obtainMessage(what));
    }

    public void sendMessageDelayed(int what, long delay) {
        sendMessageDelayed(obtainMessage(what), delay);
    }

    public void sendBtAdapterStateMsg(int state) {
        Message msg = obtainMessage(BleConnectionManager.MSG_BT_ADAPTER_STATE_CHANGED);
        msg.arg1 = state;
        sendMessage(msg);
    }

    @Override
    public void handleMessage(Message msg) {
        if (currentState != null && !currentState().handleMessage(msg)) {
            Log.w(name, "Unhandled message " + msg.what + " in state " + currentState.getName());
        }
    }

    protected String getMessageName(int what) {
        return String.valueOf(what);
    }

    protected boolean shouldLogMessage(Message msg) {
        return true;
    }

    protected void onQuiting() {}
}
