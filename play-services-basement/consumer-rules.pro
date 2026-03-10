# SPDX-FileCopyrightText: 2023 microG Project Team
# SPDX-License-Identifier: CC0-1.0

# Keep AutoSafeParcelables
-keep public class * extends org.microg.safeparcel.AutoSafeParcelable {
    @com.google.android.gms.common.internal.safeparcel.SafeParcelable$Field *;
    @org.microg.safeparcel.SafeParceled *;
}

# Keep asInterface method cause it's accessed from SafeParcel
-keepattributes InnerClasses
-keep public class * extends android.os.IInterface {
    public static * asInterface(android.os.IBinder);
}
-keep public class * extends android.os.Binder { public static *; }

# Keep name of SafeParcelables and their creators
-keepnames public class * implements com.google.android.gms.common.internal.safeparcel.SafeParcelable
-keepnames public class * implements com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter