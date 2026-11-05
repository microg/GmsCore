/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class MessageOptions extends AutoSafeParcelable {
    @SafeParceled(2)
    public int priority;

    public MessageOptions() { }

    public MessageOptions(int priority) {
        this.priority = priority;
    }

    public static final Creator<MessageOptions> CREATOR = new AutoCreator<>(MessageOptions.class);
}
