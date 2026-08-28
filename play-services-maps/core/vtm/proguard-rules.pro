-ignorewarnings
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes Signature

# Keep the VTM library and our local renderer classes named so they don't collide with
# the other renderer modules when all renderers are merged into a single APK. R8 would
# otherwise obfuscate ApplicationContextWrapper etc. from separate modules to the same
# short name (e.g. a.a), producing a duplicate-class dex error at merge time.
-keep class org.oscim.** { *; }
-keep class org.microg.gms.maps.vtm.** { *; }
-keep class org.microg.gms.maps.** { *; }