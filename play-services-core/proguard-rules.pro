# We're referencing stuff that is unknown to the system
#-libraryjar ../unifiednlp-compat/build/classes/java/main
-dontwarn java.awt.**
-dontwarn javax.annotation.**

# External libs
-dontwarn okio.**
-dontwarn com.squareup.okhttp.**
-dontwarn org.oscim.tiling.source.OkHttpEngine
-dontwarn org.oscim.tiling.source.OkHttpEngine$OkHttpFactory
-dontwarn com.caverock.androidsvg.**
-dontwarn org.slf4j.**
-dontwarn org.codehaus.jackson.**
-dontwarn com.android.location.provider.**

# Disable ProGuard Notes, they won't help here
-dontnote

# Keep dynamically loaded GMS classes
-keep public class com.google.android.gms.maps.internal.CreatorImpl { public *; }
-keep public class com.google.android.gms.common.security.ProviderInstallerImpl { public *; }
-keep public class com.google.android.gms.plus.plusone.PlusOneButtonCreatorImpl { public *; }
-keep public class com.google.android.gms.dynamic.IObjectWrapper { public *; }
-keep public class com.google.android.gms.chimera.container.DynamiteLoaderImpl { public *; }
-keep public class com.google.android.gms.dynamite.descriptors.** { public *; }
-keep public class com.google.android.gms.cast.framework.internal.CastDynamiteModuleImpl { public *; }

# Keep AutoSafeParcelables
-keep public class * extends org.microg.safeparcel.AutoSafeParcelable {
    @org.microg.safeparcel.SafeParceled *;
    @org.microg.safeparcel.SafeParcelable.Field *;
    <init>(...);
}

# Keep form data
-keepclassmembers class * {
    @org.microg.gms.common.HttpFormClient$* *;
}

# Keep our stuff
-keep class org.microg.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep asInterface method cause it's accessed from SafeParcel
-keepattributes InnerClasses
-keepclassmembers interface * extends android.os.IInterface {
    public static class *;
}
-keep public class * extends android.os.Binder { public static *; }

# Keep library info
-keep class **.BuildConfig { *; }

# Keep protobuf class builders
-keep public class com.squareup.wire.Message
-keep public class * extends com.squareup.wire.Message
-keep public class * extends com.squareup.wire.Message$Builder { public <init>(...); }

# Proguard configuration for Jackson 1.x
-keepclassmembers class * {
     @org.codehaus.jackson.annotate.* *;
}
