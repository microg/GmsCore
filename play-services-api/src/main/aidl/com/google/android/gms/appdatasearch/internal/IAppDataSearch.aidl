package com.google.android.gms.appdatasearch.internal;

import com.google.android.gms.appdatasearch.CorpusStatus;
import com.google.android.gms.appdatasearch.PIMEUpdateResponse;
import com.google.android.gms.appdatasearch.RequestIndexingSpecification;
import com.google.android.gms.appdatasearch.SuggestionResults;
import com.google.android.gms.appdatasearch.SuggestSpecification;

interface IAppDataSearch {
    SuggestionResults getSuggestions(String var1, String packageName, in String[] accounts, int maxNum, in SuggestSpecification specs) = 1;
    boolean requestIndexing(String packageName, String accountName, long l, in RequestIndexingSpecification specs) = 3;
    CorpusStatus getStatus(String packageName, String accountName) = 4;
    PIMEUpdateResponse requestPIMEUpdate(String s1, String s2, int i, in byte[] bs) = 34;
}
