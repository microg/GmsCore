package com.google.android.gms.semanticlocation.service;

import android.app.PendingIntent;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.semanticlocation.SemanticLocationEventRequest;
import com.google.android.gms.semanticlocation.internal.ISemanticLocationService;
import com.google.android.gms.semanticlocation.internal.SemanticLocationParameters;

public class SemanticLocationClientImpl extends ISemanticLocationService.Stub {

    private static final String TAG = SemanticLocationClientImpl.class.getSimpleName();

    @Override
    public void registerSemanticLocationEventsOperation(SemanticLocationParameters semanticLocationParameters, IStatusCallback callback, SemanticLocationEventRequest semanticLocationEventRequest, PendingIntent pendingIntent) throws RemoteException {
        Log.d(TAG, "registerSemanticLocationEventsOperation: " + semanticLocationParameters);
    }

    @Override
    public void setIncognitoModeOperation(SemanticLocationParameters semanticLocationParameters, IStatusCallback callback, boolean mode) throws RemoteException {
        Log.d(TAG, "setIncognitoModeOperation: " + semanticLocationParameters);
    }

    @Override
    public void unregisterSemanticLocationEventsOperation(SemanticLocationParameters semanticLocationParameters, IStatusCallback callback, PendingIntent pendingIntent) throws RemoteException {
        Log.d(TAG, "unregisterSemanticLocationEventsOperation: " + semanticLocationParameters);
    }
}
