package com.google.android.gms.auth.api.internal;

import com.google.android.gms.auth.api.internal.IAuthCallbacks;
//import com.google.android.gms.auth.api.proxy.ProxyGrpcRequest;
import com.google.android.gms.auth.api.proxy.ProxyRequest;

interface IAuthService {
    void performProxyRequest(IAuthCallbacks callbacks, in ProxyRequest request) = 0;
//    void performProxyGrpcRequest(IAuthCallback callbacks, in ProxyGrpcRequest request) = 1;
    void getSpatulaHeader(IAuthCallbacks callbacks) = 2;
}
