package com.google.android.gms.common.api;

public class Api<O extends Api.ApiOptions> {
    
    public interface ApiOptions {
        public interface HasOptions extends ApiOptions {
            
        }
        public interface NotRequiredOptions extends ApiOptions {
            
        }
    }
}
