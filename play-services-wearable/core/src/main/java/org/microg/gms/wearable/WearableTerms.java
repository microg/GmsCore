package org.microg.gms.wearable;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class WearableTerms {
    public static final int CONTEXT_UNSUPERVISED = 0;
    public static final int CONTEXT_SUPERVISED = 1;

    public static final int TOS = 0;
    public static final int LOGGING = 1;
    public static final int CLOUDSYNC = 2;
    public static final int LOCATION = 3;
    public static final int UPDATES = 4;
    public static final int BACKUP = 5;

    public static final class TermDef {
        public final int termType;
        public final boolean explicit;
        private final String name;
        private final String fallbackTitle;
        private final String fallbackDescription;

        TermDef(int termType, String name, boolean explicit, String fallbackTitle, String fallbackDescription) {
            this.termType = termType;
            this.name = name;
            this.explicit = explicit;
            this.fallbackTitle = fallbackTitle;
            this.fallbackDescription = fallbackDescription;
        }

        public String getTitle(Context context) {
            return lookup(context, "wearable_tos_" + name + "_title", fallbackTitle);
        }

        public String getDescription(Context context) {
            return lookup(context, "wearable_tos_" + name + "_description", fallbackDescription);
        }

        public int getOptInType() {
            return (termType == LOGGING || termType == CLOUDSYNC || termType == LOCATION) ? termType : 0;
        }

        private static String lookup(Context context, String resName, String fallback) {
            int id = context.getResources().getIdentifier(resName, "string", context.getPackageName());
            return id != 0 ? context.getString(id) : fallback;
        }
    }

    private static final TermDef T_TOS = new TermDef(TOS, "tos", false,
            "Set up your watch",
            "Your watch connects to this phone through microG. The choices below are saved on this phone and shared with your watch. You can change them later.");
    private static final TermDef T_LOCATION = new TermDef(LOCATION, "location", true,
            "Location",
            "Let your watch use this phone's location, for example for weather, maps and activity tracking.");
    private static final TermDef T_LOGGING = new TermDef(LOGGING, "logging", true,
            "Diagnostics",
            "Allow diagnostic data from your watch to be collected to help improve its software.");
    private static final TermDef T_BACKUP = new TermDef(BACKUP, "backup", true,
            "Backup",
            "Back up your watch's data with your account so it can be restored on a new watch.");
    private static final TermDef T_UPDATES = new TermDef(UPDATES, "updates", false,
            "Updates",
            "Your watch may receive system and app updates.");
    private static final TermDef T_CLOUDSYNC = new TermDef(CLOUDSYNC, "cloudsync", true,
            "Wi-Fi and cloud sync",
            "Let your watch sync over Wi-Fi and mobile networks when it is not connected to this phone.");

    public static List<TermDef> forContext(int termsContext) {
        switch (termsContext) {
            case CONTEXT_UNSUPERVISED:
                return Collections.unmodifiableList(Arrays.asList(T_TOS, T_LOCATION, T_LOGGING, T_BACKUP, T_UPDATES, T_CLOUDSYNC));
            case CONTEXT_SUPERVISED:
                return Collections.unmodifiableList(Arrays.asList(T_TOS, T_LOCATION, T_LOGGING, T_UPDATES));
            default:
                return null;
        }
    }

    private WearableTerms() {}
}