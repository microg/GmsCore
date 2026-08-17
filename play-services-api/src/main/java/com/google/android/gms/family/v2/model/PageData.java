/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.v2.model;

import android.os.Parcelable;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class PageData extends AutoSafeParcelable {
    @Field(1)
    public int version = 1;
    @Field(2)
    public HashMap<Integer, String> sectionMap = new HashMap<>();
    @Field(3)
    public HashMap<String, HelpData> helpMap = new HashMap<>();
    @Field(4)
    public ArrayList<BulletPoint> bulletPoints = new ArrayList<>();

    public PageData() {}

    public PageData(HashMap<Integer, String> sectionMap, HashMap<String, HelpData> helpMap, ArrayList<BulletPoint> bulletPoints) {
        this.sectionMap = sectionMap;
        this.helpMap = helpMap;
        this.bulletPoints = bulletPoints;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PageData)) return false;
        PageData other = (PageData) obj;
        return version == other.version &&
                Objects.equals(sectionMap, other.sectionMap) &&
                Objects.equals(helpMap, other.helpMap) &&
                Objects.equals(bulletPoints, other.bulletPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, sectionMap, helpMap, bulletPoints);
    }

    @Override
    public String toString() {
        return "PageData{" +
                "version=" + version +
                ", sectionMap=" + sectionMap +
                ", helpMap=" + helpMap +
                ", bulletPoints=" + bulletPoints +
                '}';
    }

    public static final Parcelable.Creator<PageData> CREATOR = findCreator(PageData.class);
}
