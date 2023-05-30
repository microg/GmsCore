/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel.test.auto;

import android.os.Parcel;
import android.os.Parcelable;

import org.junit.Test;
import org.microg.safeparcel.test.mock.MockParcel;

import static org.junit.Assert.assertEquals;

public class AutoTests {
    static <T extends Parcelable> T remarshal(T orig, Parcelable.Creator<T> tCreator) {
        Parcel parcel = MockParcel.obtain();
        orig.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        parcel = MockParcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        T re = tCreator.createFromParcel(parcel);
        parcel.recycle();
        return re;
    }

    @Test
    public void bar() {
        Bar bar1 = new Bar(12);
        Bar bar2 = remarshal(bar1, Bar.CREATOR);
        assertEquals(bar1, bar2);
    }

    @Test
    public void foo() {
        Foo foo1 = new Foo(4);
        foo1.string = "Hello";
        foo1.stringList.add("Hello2");
        foo1.stringStringMap.put("Hello3", "Hello4");
        foo1.bar = new Bar(5);
        foo1.barList.add(foo1.bar);
        foo1.barArray = new Bar[]{foo1.bar};
        foo1.intList.add(2);
        foo1.intList2.add(3);
        foo1.floatArray[0] = 1f;
        foo1.floatArray[1] = 3f;
        foo1.floatArray[2] = 3f;
        foo1.floatArray[3] = 7f;
        foo1.intArray[0] = 1337;
        foo1.byteArray[0] = 42;
        foo1.byteArrayArray[0][0] = 24;
        Foo foo2 = remarshal(foo1, Foo.CREATOR);
        assertEquals(foo1, foo2);
    }
}

