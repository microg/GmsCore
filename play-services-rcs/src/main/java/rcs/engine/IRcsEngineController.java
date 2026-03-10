/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcs.engine;

import android.os.IBinder;
import android.os.IInterface;
import com.google.android.ims.rcs.engine.RcsEngineLifecycleServiceResult;

/**
 * IRcsEngineController interface - following Google's exact pattern from reverse-engineered code
 */
public interface IRcsEngineController extends IInterface {
    
    /**
     * Initialize RCS engine for specific subscription ID
     */
    RcsEngineLifecycleServiceResult initialize(int subId, int flags);
    
    /**
     * Destroy RCS engine for specific subscription ID
     */
    RcsEngineLifecycleServiceResult destroy(int subId);
    
    /**
     * Trigger start of RCS stack
     */
    RcsEngineLifecycleServiceResult triggerStartRcsStack(int subId);
    
    /**
     * Trigger stop of RCS stack
     */
    RcsEngineLifecycleServiceResult triggerStopRcsStack(int subId);
    
    /**
     * Initialize and start RCS transport
     */
    RcsEngineLifecycleServiceResult initializeAndStartRcsTransport(com.google.android.ims.rcsservice.lifecycle.InitializeAndStartRcsTransportRequest request);
    
    /**
     * Stop all RCS transports except specified one
     */
    RcsEngineLifecycleServiceResult stopAllRcsTransportsExcept(com.google.android.ims.rcsservice.lifecycle.StopAllRcsTransportsExceptRequest request);
    
    /**
     * Abstract Stub class for Binder implementation
     */
    abstract class Stub extends android.os.Binder implements IRcsEngineController {
        public static IRcsEngineController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IRcsEngineController) {
                return (IRcsEngineController) iin;
            }
            return new Proxy(obj);
        }
        
        public IBinder asBinder() {
            return this;
        }
        
        private static class Proxy implements IRcsEngineController {
            private IBinder mRemote;
            
            Proxy(IBinder remote) {
                mRemote = remote;
            }
            
            @Override
            public IBinder asBinder() {
                return mRemote;
            }
            
            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }
            
            @Override
            public RcsEngineLifecycleServiceResult initialize(int subId, int flags) {
                // Forward to actual implementation - this is critical for Google Messages!
                try {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(subId);
                    data.writeInt(flags);
                    mRemote.transact(TRANSACTION_initialize, data, reply, 0);
                    reply.readException();
                    RcsEngineLifecycleServiceResult result = RcsEngineLifecycleServiceResult.CREATOR.createFromParcel(reply);
                    reply.recycle();
                    data.recycle();
                    return result;
                } catch (Exception e) {
                    return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.ERROR_UNKNOWN, "Proxy error: " + e.getMessage());
                }
            }
            
            @Override
            public RcsEngineLifecycleServiceResult destroy(int subId) {
                // Forward to actual implementation
                try {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(subId);
                    mRemote.transact(TRANSACTION_destroy, data, reply, 0);
                    reply.readException();
                    RcsEngineLifecycleServiceResult result = RcsEngineLifecycleServiceResult.CREATOR.createFromParcel(reply);
                    reply.recycle();
                    data.recycle();
                    return result;
                } catch (Exception e) {
                    return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.ERROR_UNKNOWN, "Proxy error: " + e.getMessage());
                }
            }
            
            @Override
            public RcsEngineLifecycleServiceResult triggerStartRcsStack(int subId) {
                // Forward to actual implementation
                try {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(subId);
                    mRemote.transact(TRANSACTION_triggerStartRcsStack, data, reply, 0);
                    reply.readException();
                    RcsEngineLifecycleServiceResult result = RcsEngineLifecycleServiceResult.CREATOR.createFromParcel(reply);
                    reply.recycle();
                    data.recycle();
                    return result;
                } catch (Exception e) {
                    return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.ERROR_UNKNOWN, "Proxy error: " + e.getMessage());
                }
            }
            
            @Override
            public RcsEngineLifecycleServiceResult triggerStopRcsStack(int subId) {
                // Forward to actual implementation
                try {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(subId);
                    mRemote.transact(TRANSACTION_triggerStopRcsStack, data, reply, 0);
                    reply.readException();
                    RcsEngineLifecycleServiceResult result = RcsEngineLifecycleServiceResult.CREATOR.createFromParcel(reply);
                    reply.recycle();
                    data.recycle();
                    return result;
                } catch (Exception e) {
                    return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.ERROR_UNKNOWN, "Proxy error: " + e.getMessage());
                }
            }
            
            @Override
            public RcsEngineLifecycleServiceResult initializeAndStartRcsTransport(com.google.android.ims.rcsservice.lifecycle.InitializeAndStartRcsTransportRequest request) {
                // Forward to actual implementation
                try {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    if (request != null) {
                        data.writeInt(1);
                        request.writeToParcel(data, 0);
                    } else {
                        data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_initializeAndStartRcsTransport, data, reply, 0);
                    reply.readException();
                    RcsEngineLifecycleServiceResult result = RcsEngineLifecycleServiceResult.CREATOR.createFromParcel(reply);
                    reply.recycle();
                    data.recycle();
                    return result;
                } catch (Exception e) {
                    return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.ERROR_UNKNOWN, "Proxy error: " + e.getMessage());
                }
            }
            
            @Override
            public RcsEngineLifecycleServiceResult stopAllRcsTransportsExcept(com.google.android.ims.rcsservice.lifecycle.StopAllRcsTransportsExceptRequest request) {
                // Forward to actual implementation
                try {
                    android.os.Parcel data = android.os.Parcel.obtain();
                    android.os.Parcel reply = android.os.Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    if (request != null) {
                        data.writeInt(1);
                        request.writeToParcel(data, 0);
                    } else {
                        data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_stopAllRcsTransportsExcept, data, reply, 0);
                    reply.readException();
                    RcsEngineLifecycleServiceResult result = RcsEngineLifecycleServiceResult.CREATOR.createFromParcel(reply);
                    reply.recycle();
                    data.recycle();
                    return result;
                } catch (Exception e) {
                    return new RcsEngineLifecycleServiceResult(RcsEngineLifecycleServiceResult.ERROR_UNKNOWN, "Proxy error: " + e.getMessage());
                }
            }
        }
        
        static final String DESCRIPTOR = "com.google.android.ims.rcs.engine.IRcsEngineController";
    static final int TRANSACTION_initialize = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_destroy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_triggerStartRcsStack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_triggerStopRcsStack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_initializeAndStartRcsTransport = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_stopAllRcsTransportsExcept = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    }
}
