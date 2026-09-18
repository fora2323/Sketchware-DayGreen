# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==== Tambahan: cegah class & kode project kena strip/obfuscate ====

# Jangan obfuscate nama class/method sama sekali (gampang buat debug crash log juga)
-dontobfuscate

# Jangan shrink/hapus class apapun di package project sendiri
-keep class ** { *; }
-keepclassmembers class ** { *; }

# Jaga semua constructor (kepake buat inflate View dari XML/reflection)
-keepclassmembers class * {
    public <init>(...);
}

# Jaga Activity, Service, BroadcastReceiver, ContentProvider, Application (entry point via manifest)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# Jaga custom View (constructor dipanggil via reflection pas inflate XML)
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Parcelable & Serializable (dibutuhin lewat reflection field CREATOR)
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Enum (dipake reflection buat values()/valueOf())
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native methods (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Annotation (kepake framework kayak Gson/Retrofit)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations

# WebView JS interface (uncomment & isi kalo pake addJavascriptInterface)
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Line number tetep kejaga buat debug stack trace
-keepattributes SourceFile,LineNumberTable

# R class (resource ID constants) jangan sampe ke-strip
-keep class **.R
-keep class **.R$* { *; }