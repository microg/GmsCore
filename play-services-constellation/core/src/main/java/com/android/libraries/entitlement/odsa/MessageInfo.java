/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.odsa;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * MSG information described in GSMA Service Entitlement Configuration section 6.5.5 table 43.
 */
@AutoValue
public abstract class MessageInfo {
    /** Indicates the button or freetext is absent. */
    public static final String MESSAGE_VALUE_ABSENT = "0";

    /** Indicates the button or freetext is present. */
    public static final String MESSAGE_VALUE_PRESENT = "1";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            MESSAGE_VALUE_ABSENT,
            MESSAGE_VALUE_PRESENT
    })
    public @interface MessageValue {
    }

    /** The message that is displayed to the user. */
    @NonNull
    public abstract String message();

    /**
     * Whether an {@code Accept} button is shown with the {@link #message()} on device UI.
     * The action associated with the {@code Accept} button on the device/client is to clear the
     * message box.
     */
    @NonNull
    @MessageValue
    public abstract String acceptButton();

    /** The label for the {@code Accept} button to be presented to the user. */
    @NonNull
    public abstract String acceptButtonLabel();

    /**
     * Whether a {@code Reject} button is shown with the {@link #message()} on device UI.
     * The action associated with the {@code Reject} button on the device/client is to revert the
     * configured services to their defined default behaviour.
     */
    @NonNull
    @MessageValue
    public abstract String rejectButton();

    /** The label for the {@code Reject} button to be presented to the user. */
    @NonNull
    public abstract String rejectButtonLabel();

    /** Whether a free text entry field is shown with the message on device UI. */
    @NonNull
    @MessageValue
    public abstract String acceptFreetext();

    /** Returns builder of {@link MessageInfo}. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_MessageInfo.Builder()
                .setMessage("")
                .setAcceptButton("")
                .setAcceptButtonLabel("")
                .setRejectButton("")
                .setRejectButtonLabel("")
                .setAcceptFreetext("");
    }

    /** Builder of MessageInfo. */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Set the message that is displayed to the user.
         *
         * @param message The message to display to the user.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setMessage(@NonNull String message);

        /**
         * Set whether an {@code Accept} button is shown with the {@link #message()} on device UI.
         *
         * @param button {@link #MESSAGE_VALUE_PRESENT} to show and
         *               {@link #MESSAGE_VALUE_ABSENT} to hide.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setAcceptButton(@NonNull String button);

        /**
         * Set the label for the {@code Accept} button to be presented to the user.
         *
         * @param label The label for the {@code Accept} button.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setAcceptButtonLabel(@NonNull String label);

        /**
         * Set whether an {@code Reject} button is shown with the {@link #message()} on device UI.
         *
         * @param button {@link #MESSAGE_VALUE_PRESENT} to show and
         *               {@link #MESSAGE_VALUE_ABSENT} to hide.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setRejectButton(@NonNull String button);

        /**
         * Set the label for the {@code Reject} button to be presented to the user.
         *
         * @param label The label for the {@code Reject} button.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setRejectButtonLabel(@NonNull String label);

        /**
         * Set whether a free text entry field is shown with the message on device UI.
         *
         * @param accept {@link #MESSAGE_VALUE_PRESENT} to show and
         *               {@link #MESSAGE_VALUE_ABSENT} to hide.
         * @return The builder.
         */
        @NonNull
        public abstract Builder setAcceptFreetext(@NonNull String accept);

        /** Build the MessageInfo object. */
        @NonNull
        public abstract MessageInfo build();
    }
}
