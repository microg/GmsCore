package com.google.android.gms.search.corpora.internal;

import com.google.android.gms.search.corpora.RequestIndexingResponse;
import com.google.android.gms.search.corpora.GetCorpusStatusResponse;

interface ISearchCorporaCallbacks {
    void onRequestIndexing(in RequestIndexingResponse response) = 1;
    void onGetCorpusStatus(in GetCorpusStatusResponse response) = 3;
}
