package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class Term extends AutoSafeParcelable {
    @SafeParceled(1)
    public int termType;
    @SafeParceled(2)
    public String description;
    @SafeParceled(3)
    public boolean isExplicitConsent;
    @SafeParceled(4)
    public String title;
    @SafeParceled(5)
    public String unknown5;
    @SafeParceled(6)
    public int optInType;

    private Term() {}

    public Term(int termType, String description, boolean isExplicitConsent, String title, String unknown5, int optInType) {
        this.termType = termType;
        this.description = description;
        this.isExplicitConsent = isExplicitConsent;
        this.title = title;
        this.unknown5 = unknown5;
        this.optInType = optInType;
    }

    public static final Creator<Term> CREATOR = new AutoCreator<Term>(Term.class);
}