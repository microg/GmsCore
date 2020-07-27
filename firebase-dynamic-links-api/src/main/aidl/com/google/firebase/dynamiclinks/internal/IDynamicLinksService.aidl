package com.google.firebase.dynamiclinks.internal;


import com.google.firebase.dynamiclinks.internal.IDynamicLinksCallbacks;

import android.os.Bundle;


interface IDynamicLinksService {
    void getInitialLink(IDynamicLinksCallbacks callback, String var2) = 0;
    void func2(IDynamicLinksCallbacks callback, in Bundle var2) = 1;
}
