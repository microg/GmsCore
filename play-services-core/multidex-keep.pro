# Keep Conscrypt in the primary dex file otherwise it will error out on legacy Android versions
-keep class com.google.android.gms.common.security.ProviderInstallerImpl { *; }
-keep class com.google.android.gms.org.conscrypt.** { *; }
