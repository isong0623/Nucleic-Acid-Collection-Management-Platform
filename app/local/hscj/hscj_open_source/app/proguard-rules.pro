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

-dontshrink
-dontoptimize

-keep class com.dreaming.hscj.utils.algorithm.** {*;}
-keep class com.dreaming.hscj.Constants{*;}
-keep class okhttp3.** {*;}
-keep class okio.** {*;}

-keep class org.** {*;}

-keep class priv.** {*;}

-keep class com.dreaming.hscj.template.api.impl.ApiListener{*;}
-keep class com.dreaming.hscj.template.api.ApiTemplate{*;}
-keep class com.dreaming.hscj.template.api.impl.Api{
    public void doRequest(...);
}
-keep class com.dreaming.hscj.template.database.DatabaseTemplate{*;}
-keep class com.dreaming.hscj.template.Template{
    ** read(***);
    ** apiOf(***);
}

-keep class **{
     void set*(***);
     *** get*();
}