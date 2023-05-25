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

package com.google.android.gms.clearcut;

import android.util.Base64;

import com.google.android.gms.clearcut.internal.LogVerifierResultParcelable;
import com.google.android.gms.phenotype.ExperimentToken;
import com.google.android.gms.phenotype.GenericDimension;
import com.google.android.gms.clearcut.internal.PlayLoggerContext;

import org.microg.safeparcel.AutoSafeParcelable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;

public class LogEventParcelable extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;

    @Field(2)
    public final PlayLoggerContext context;

    @Field(3)
    public final byte[] bytes;

    @Field(4)
    public final int[] testCodes;

    @Field(5)
    public final String[] mendelPackages;

    @Field(6)
    public final int[] experimentIds;

    @Field(7)
    public final byte[][] experimentTokens;

    @Field(8)
    public final boolean addPhenotypeExperimentTokens;

    @Field(9)
    public final ExperimentToken[] experimentTokenParcelables;

    @Field(10)
    public final GenericDimension[] genericDimensions;

    @Field(11)
    public final LogVerifierResultParcelable logVerifierResult;

    @Field(12)
    private String[] mendelPackagesToFilter;

    @Field(13)
    public int eventCode;

    private LogEventParcelable() {
        context = null;
        bytes = null;
        testCodes = experimentIds = null;
        mendelPackages = null;
        experimentTokens = null;
        addPhenotypeExperimentTokens = true;
        experimentTokenParcelables = null;
        genericDimensions = null;
        logVerifierResult = null;
    }

    public LogEventParcelable(PlayLoggerContext context, byte[] bytes, int[] testCodes, String[] mendelPackages, int[] experimentIds, byte[][] experimentTokens, boolean addPhenotypeExperimentTokens) {
        this.context = context;
        this.bytes = bytes;
        this.testCodes = testCodes;
        this.mendelPackages = mendelPackages;
        this.experimentIds = experimentIds;
        this.experimentTokens = experimentTokens;
        this.addPhenotypeExperimentTokens = addPhenotypeExperimentTokens;
        this.experimentTokenParcelables = null;
        this.genericDimensions = null;
        this.logVerifierResult = null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LogEventParcelable[")
                .append(versionCode).append(", ").append(context)
                .append(", LogEventBytes: ").append(getBytesAsString());
        if (testCodes != null) sb.append(", TestCodes: ").append(Arrays.toString(testCodes));
        if (mendelPackages != null)
            sb.append(", MendelPackages: ").append(Arrays.toString(mendelPackages));
        if (experimentIds != null)
            sb.append(", ExperimentIds: ").append(Arrays.toString(experimentIds));
        if (experimentTokens != null)
            sb.append(", ExperimentTokens: ").append(Arrays.toString(experimentTokens));
        return sb.append(", AddPhenotypeExperimentTokens: ").append(addPhenotypeExperimentTokens)
                .append(']').toString();
    }

    private String getBytesAsString() {
        if (bytes == null) return "null";
        try {
            CharsetDecoder d = Charset.forName("US-ASCII").newDecoder();
            CharBuffer r = d.decode(ByteBuffer.wrap(bytes));
            return r.toString();
        } catch (Exception e) {
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        }
    }

    public static final Creator<LogEventParcelable> CREATOR = new AutoCreator<LogEventParcelable>(LogEventParcelable.class);
}
