/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.icing;

import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.appdatasearch.CorpusStatus;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.search.corpora.ClearCorpusRequest;
import com.google.android.gms.search.corpora.GetCorpusInfoRequest;
import com.google.android.gms.search.corpora.GetCorpusStatusRequest;
import com.google.android.gms.search.corpora.GetCorpusStatusResponse;
import com.google.android.gms.search.corpora.RequestIndexingRequest;
import com.google.android.gms.search.corpora.RequestIndexingResponse;
import com.google.android.gms.search.corpora.internal.ISearchCorporaCallbacks;
import com.google.android.gms.search.corpora.internal.ISearchCorporaService;

import java.util.HashMap;
import java.util.Map;

public class SearchCorporaImpl extends ISearchCorporaService.Stub {
    private static final String TAG = "GmsIcingCorporaImpl";

    // We count the sequence number here to make clients happy.
    private final Map<String, Long> corpusSequenceNumbers = new HashMap<String, Long>();

    @Override
    public void requestIndexing(RequestIndexingRequest request, ISearchCorporaCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "requestIndexing: " + request);
        corpusSequenceNumbers.put(request.packageName + "/" + request.corpus, request.sequenceNumber);
        callbacks.onRequestIndexing(new RequestIndexingResponse(Status.SUCCESS, true));
    }

    @Override
    public void clearCorpus(ClearCorpusRequest request, ISearchCorporaCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "clearCorpus");
    }

    @Override
    public void getCorpusStatus(GetCorpusStatusRequest request, ISearchCorporaCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getCorpusStatus: " + request);
        CorpusStatus status = new CorpusStatus();
        String numIndex = request.packageName + "/" + request.corpus;
        if (corpusSequenceNumbers.containsKey(numIndex)) {
            status.found = true;
            status.lastIndexedSeqno = corpusSequenceNumbers.get(numIndex);
            status.lastCommittedSeqno = status.lastIndexedSeqno;
        }
        callbacks.onGetCorpusStatus(new GetCorpusStatusResponse(Status.SUCCESS, status));
    }

    @Override
    public void getCorpusInfo(GetCorpusInfoRequest request, ISearchCorporaCallbacks callbacks) throws RemoteException {
        Log.d(TAG, "getCorpusInfo");
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (super.onTransact(code, data, reply, flags)) return true;
        Log.d(TAG, "onTransact [unknown]: " + code + ", " + data + ", " + flags);
        return false;
    }
}
