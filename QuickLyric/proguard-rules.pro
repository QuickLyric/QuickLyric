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
-keepattributes Signature, Exceptions, Annotation

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

-keep class org.jsoup.** { *; }
-keep class retrofit2.** { *; }
-keep class org.jaudiotagger.** { *; }
-keep public class com.google.** { *; }
-keep class com.drivemode.spotify.** { *; }

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

