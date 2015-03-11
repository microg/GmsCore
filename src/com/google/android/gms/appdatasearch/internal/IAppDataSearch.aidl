package com.google.android.gms.appdatasearch.internal;

import com.google.android.gms.appdatasearch.CorpusStatus;
import com.google.android.gms.appdatasearch.SuggestionResults;
import com.google.android.gms.appdatasearch.SuggestSpecification;

interface IAppDataSearch {
    SuggestionResults getSuggestions(String var1, String packageName, in String[] accounts, int maxNum, in SuggestSpecification specs) = 1;
    CorpusStatus getStatus(String packageName, String accountName) = 4;
}
