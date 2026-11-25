# Keep Xray/libv2ray JNI interfaces
-keep class go.** { *; }
-keep class libv2ray.** { *; }
-keep class tun2socks.** { *; }
-keep class com.eternalvpn.app.core.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
