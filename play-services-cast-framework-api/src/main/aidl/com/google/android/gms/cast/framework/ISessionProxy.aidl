package com.google.android.gms.cast.framework;

import com.google.android.gms.dynamic.IObjectWrapper;

// TODO: Functionality still needs to be determined
interface ISessionProxy {
    IObjectWrapper getWrappedThis() = 0;
    void method1(in Bundle paramBundle) = 1;
    void method2(in Bundle paramBundle) = 2;
    void method3(boolean paramBoolean) = 3;
    long method4() = 4;
    int method5() = 5;
    void method6(in Bundle paramBundle) = 6;
    void method7(in Bundle paramBundle) = 7;
}
