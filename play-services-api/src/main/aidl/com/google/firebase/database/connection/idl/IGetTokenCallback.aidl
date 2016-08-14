package com.google.firebase.database.connection.idl;

interface IGetTokenCallback {
    void zero(String s) = 0;
    void onError(String s) = 1;
}