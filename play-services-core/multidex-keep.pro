# Make sure maps is in the primary dex file
-keep class com.google.android.gms.maps.** { *; }
-keep class org.microg.gms.maps.** { *; }
-keep class com.mapbox.** { *; }
-keep class org.oscim.** { *; }

# Make sure these classes are in the primary dex file otherwise Conscrypt will error out on legacy Android versions
-keep class com.google.android.gms.common.security.ProviderInstallerImpl { *; }
-keep class com.google.android.gms.org.conscrypt.** { *; }
