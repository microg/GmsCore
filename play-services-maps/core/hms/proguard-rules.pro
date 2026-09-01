-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.huawei.hianalytics.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}

# Keep our local renderer classes named so they don't collide with other renderer
# modules when all renderers are merged into a single APK (R8 would otherwise
# obfuscate MapContext/ApplicationContextWrapper from separate modules to the same
# short name, e.g. a.a).
-keep class org.microg.gms.maps.hms.** { *; }
-keep class org.microg.gms.maps.** { *; }