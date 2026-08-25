package com.google.android.gms.search.queries.internal;

import com.google.android.gms.search.queries.QueryRequest;
import com.google.android.gms.search.queries.internal.ISearchQueriesCallbacks;

interface ISearchQueriesService {
    void query(in QueryRequest request, ISearchQueriesCallbacks callbacks) = 1;
}
