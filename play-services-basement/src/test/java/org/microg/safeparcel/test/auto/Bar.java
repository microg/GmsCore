/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel.test.auto;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Objects;

public class Bar extends AutoSafeParcelable {
    @Field(1)
    public long another;

    private Bar() {
    }

    public Bar(long another) {
        this.another = another;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bar bar = (Bar) o;
        return another == bar.another;
    }

    @Override
    public int hashCode() {
        return Objects.hash(another);
    }

    public static Creator<Bar> CREATOR = new AutoCreator<>(Bar.class);
}
