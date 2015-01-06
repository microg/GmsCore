package com.google.android.auth;

interface IAuthManagerService {
    Bundle getToken(String accountName, String scope, in Bundle extras);
    Bundle clearToken(String token, in Bundle extras);
}
