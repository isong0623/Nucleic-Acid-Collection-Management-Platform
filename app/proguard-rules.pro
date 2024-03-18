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

-optimizationpasses 5

-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

-dontwarn android.support.v4.**
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
-keep class android.support.v4.** { *; }
-keep public class * extends android.app.Application

-dontwarn android.support.v7.**
-keep class android.support.v7.internal.** { *; }
-keep interface android.support.v7.internal.** { *; }
-keep class android.support.v7.** { *; }

-keep class * extends android.app.Activity
-keep class * extends android.app.Fragment
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends android.app.backup.BackupAgentHelper
-keep class * extends android.preference.Preference


-keep class com.dreaming.security.Defender{*;}
-keep class com.dreaming.hscj.base.contract.BaseView{*;}
-keep class com.dreaming.hscj.base.contract.BasePresenter{*;}
-keep class com.dreaming.**.**Presenter{
    <init>(***);
}
-keep interface com.dreaming.**.**Contract$View{
    <init>(***);
}
-keep class com.dreaming.**.**Contract$Presenter{
    <init>(***);
}

-keep class net.**{*;}
-keep class com.leon.** {*;}
-keep class com.baidu.** {*;}
-keep class com.android.**{*;}
-keep class system.**{*;}

-keep class com.bea.** {*;}
-keep class com.wutka.** {*;}
-keep class org.** {*;}
-keep class apex.**{*;}
-keep class aavax.**{*;}
-keep class repackage.**{*;}
-keep class com.microsoft.** {*;}
-keep class schemaorg_apache_xmlbeans.**{*;}
-keep class schemasMicrosoftComVml.** {*;}
-keep class schemasMicrosoftComOfficeOffice.** {*;}
-keep class schemasMicrosoftComOfficeExcel.** {*;}
-keep class schemasMicrosoftComVml.** {*;}

-keep class com.belerweb.** {*;}
-keep class net.** {*;}
-keep class com.jakewharton.** {*;}
-keep class com.lzy.** {*;}
#-keep class com.github.** {*;}
-keep class com.google.** {*;}
-keep class com.journeyapps.** {*;}
-keep class com.scwang.** {*;}
-keep class com.air4.** {*;}
-keep class butterknife.** {*;}
-keep class priv.** {*;}

-keep class okio.** {*;}
-keep class okhttp3.** {*;}
-keep class com.hp.** {*;}
-keep class com.tencent.** {*;}
-keep class MTT.** {*;}
-keep class android.** {*;}
-keep class androidx.** {*;}
-keep class java.** {*;}
-keep class javax.** {*;}

-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context,android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context,android.util.AttributeSet,int);
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
}
