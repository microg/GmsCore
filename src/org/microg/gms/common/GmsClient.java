package org.microg.gms.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.common.internal.IGmsServiceBroker;

import org.microg.gms.common.api.ApiConnection;

public abstract class GmsClient<I extends IInterface> implements ApiConnection {
    private static final String TAG = "GmsClient";

    private final Context context;
    private final GoogleApiClient.ConnectionCallbacks callbacks;
    private final GoogleApiClient.OnConnectionFailedListener connectionFailedListener;
    private ConnectionState state = ConnectionState.CONNECTED;
    private ServiceConnection serviceConnection;
    private I serviceInterface;

    public GmsClient(Context context, GoogleApiClient.ConnectionCallbacks callbacks,
            GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {
        this.context = context;
        this.callbacks = callbacks;
        this.connectionFailedListener = connectionFailedListener;
    }

    protected abstract String getActionString();

    protected abstract void onConnectedToBroker(IGmsServiceBroker broker, GmsCallbacks callbacks)
            throws RemoteException;

    protected abstract I interfaceFromBinder(IBinder binder);

    @Override
    public void connect() {
        state = ConnectionState.CONNECTING;
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) !=
                ConnectionResult.SUCCESS) {
            state = ConnectionState.NOT_CONNECTED;
        } else {
            if (serviceConnection != null) {
                MultiConnectionKeeper.getInstance(context)
                        .unbind(getActionString(), serviceConnection);
            }
            serviceConnection = new GmsServiceConnection();
            MultiConnectionKeeper.getInstance(context).bind(getActionString(),
                    serviceConnection);
        }
    }

    @Override
    public void disconnect() {
        serviceInterface = null;
        if (serviceConnection != null) {
            MultiConnectionKeeper.getInstance(context).unbind(getActionString(), serviceConnection);
            serviceConnection = null;
        }
        state = ConnectionState.NOT_CONNECTED;
    }

    @Override
    public boolean isConnected() {
        return state == ConnectionState.CONNECTED;
    }

    public Context getContext() {
        return context;
    }

    public I getServiceInterface() {
        return serviceInterface;
    }

    private enum ConnectionState {
        NOT_CONNECTED, CONNECTING, CONNECTED, ERROR
    }

    private class GmsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try {
                Log.d(TAG, "Connecting to broker for " + componentName);
                onConnectedToBroker(IGmsServiceBroker.Stub.asInterface(iBinder),
                        new GmsCallbacks());
            } catch (RemoteException e) {
                disconnect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            state = ConnectionState.ERROR;
        }
    }

    public class GmsCallbacks extends IGmsCallbacks.Stub {

        @Override
        public void onPostInitComplete(int statusCode, IBinder binder, Bundle params)
                throws RemoteException {
            serviceInterface = interfaceFromBinder(binder);
            callbacks.onConnected(params);
        }
    }

}
