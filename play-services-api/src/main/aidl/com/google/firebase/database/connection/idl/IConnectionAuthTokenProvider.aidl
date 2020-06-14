package com.google.firebase.database.connection.idl;

import com.google.firebase.database.connection.idl.IGetTokenCallback;

interface IConnectionAuthTokenProvider {
    void zero(boolean var1, IGetTokenCallback var2) = 0;
}