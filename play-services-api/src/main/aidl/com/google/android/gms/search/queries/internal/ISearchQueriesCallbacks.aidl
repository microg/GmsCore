package com.google.android.gms.search.queries.internal;

import com.google.android.gms.search.queries.QueryResponse;

interface ISearchQueriesCallbacks {
    void onQuery(in QueryResponse response) = 1;
}
