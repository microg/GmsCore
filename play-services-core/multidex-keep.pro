# Make sure maps is in the primary dex file
-keep class com.google.android.gms.maps.** { *; }
-keep class org.microg.gms.maps.** { *; }
-keep class com.mapbox.** { *; }
-keep class org.oscim.** { *; }

# Keep Dynamite Loader in the primary dex file otherwise it will error out on legacy Android versions
-keep class com.google.android.gms.chimera.container.DynamiteLoaderImpl { *; }

# Keep Conscrypt in the primary dex file otherwise it will error out on legacy Android versions
-keep class com.google.android.gms.common.security.ProviderInstallerImpl { *; }
-keep class com.google.android.gms.org.conscrypt.** { *; }
