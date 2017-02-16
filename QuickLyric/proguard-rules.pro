# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
<<<<<<< HEAD
=======
-keepattributes Signature, Exceptions, Annotation

>>>>>>> d989421... Update proguard rules
-dontwarn org.jaudiotagger.**
-dontwarn com.viewpagerindicator.**
-dontwarn java.nio.file.Files
-dontwarn java.nio.file.Path
-dontwarn java.nio.file.OpenOption
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn retrofit2.**
-dontwarn com.google.appengine.**
-dontwarn rx.**
-dontwarn org.apache.**
-dontwarn android.net.http.**
-dontwarn com.android.volley.**
-dontwarn okhttp3.**

-keep class retrofit2.** { *; }
-keep class org.jaudiotagger.** { *; }
-keep public class com.google.** { *; }
-keep class com.drivemode.spotify.** { *; }
-keep class com.google.analytics.** { *; }
-keep class com.android.vending.billing.**
-keep public class com.google.ads.** {
   public *;
}
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
