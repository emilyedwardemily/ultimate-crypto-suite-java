#
# ULTIMATE CRYPTO SUITE - ProGuard obfuscation rules
#
# Applies ProGuard BEFORE packaging so that every UltimateCryptoSuite
# class that is not a hard reflection / entry-point target is renamed to
# unreadable identifiers, dead code is shrunk, and debug info is stripped.
#
# Requires proguard-core >= 9.2.0 to parse Java 25 (class version 69)
# bytecode. See pom.xml for the dependency override.
#

# ------------------------------------------------------------------
# Basic tuning
# ------------------------------------------------------------------
# -dontoptimize keeps the JavaFX Task/Dashboard subclass hierarchy intact
# (the optimization pass can emit "unresolved references to program class
# members" for Task.updateMessage/updateProgress/stop call sites).
-dontoptimize
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively

# Keep the (renamed) source file attribute + line numbers so crash reports
# remain decodable with obfuscation-map.txt, without leaking the original name.
-keepattributes SourceFile,LineNumberTable,Exceptions,InnerClasses,EnclosingMethod,*Annotation*,Signature
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------
# Entry points - MUST survive obfuscation unchanged
# ------------------------------------------------------------------
# jpackage manifest main class (invoked directly by the launcher).
-keep public class app.Launcher {
    public static void main(java.lang.String[]);
}

# JavaFX application: Application.launch(MainApp.class, ...) reflects the
# class name; its public members are also referenced from other app classes.
-keep public class app.MainApp {
    public static void main(java.lang.String[]);
    public void start(javafx.stage.Stage);
    public static void openDashboard();
    public static void showLogin();
}

# ui.LoginScreen is reflected by name from EliteService
# (Class.forName("ui.LoginScreen") + getField("USERNAME")). Its public
# static session fields are read through reflection, so keep names.
-keep class ui.LoginScreen {
    public static java.lang.String SESSION_TOKEN;
    public static java.lang.String USER_ROLE;
    public static java.lang.String USERNAME;
    public static java.lang.String SESSION_EMAIL;
    public <init>(javafx.stage.Stage);
}

# Integrity guard must stay fully intact so anti-tamper verification works.
-keep class app.TamperGuard { *; }

# ------------------------------------------------------------------
# Libraries driven by reflection / serialization
# ------------------------------------------------------------------
-keep class org.json.** { *; }
-keep,allowshrinking class com.google.gson.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class com.mongodb.** { *; }
-keep class org.bson.** { *; }
-keep class com.itextpdf.** { *; }
-keep class com.lowagie.** { *; }
-keep class com.google.zxing.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }

# Enums (values()/valueOf() are called reflectively by JSON + frameworks).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ------------------------------------------------------------------
# JavaFX reflection surface
# ------------------------------------------------------------------
# Keep any JavaFX Application subclass and Control subtypes that the
# runtime may instantiate reflectively.
-keep class * extends javafx.application.Application { *; }
-keepclassmembers class * extends javafx.scene.control.* {
    <init>(...);
}

# ------------------------------------------------------------------
# Output mapping / diagnostics
# ------------------------------------------------------------------
-dontwarn javax.annotation.**
-dontwarn javafx.**
-dontwarn org.controlsfx.**
-dontnote **
-printmapping target/obfuscation-map.txt

# ------------------------------------------------------------------
# Optional / conditional dependencies (not shipped in the runtime image)
# ------------------------------------------------------------------
-dontwarn io.netty.**
-dontwarn com.mongodb.crypt.capi.**
-dontwarn javax.security.sasl.**
-dontwarn com.amazonaws.**
-dontwarn software.amazon.awssdk.**
-dontwarn org.xerial.snappy.**
-dontwarn com.github.luben.zstd.**
-dontwarn jnr.unixsocket.**
-dontwarn org.bson.codecs.kotlin.**
-dontwarn org.bson.codecs.kotlinx.**
-dontwarn org.bouncycastle.cert.**
-dontwarn org.bouncycastle.cms.**
-dontwarn org.bouncycastle.tsp.**
-dontwarn org.bouncycastle.operator.**
-dontwarn org.bouncycastle.asn1.cms.**
-dontwarn org.bouncycastle.asn1.esf.**
-dontwarn org.bouncycastle.asn1.cmp.**
-dontwarn org.bouncycastle.asn1.isismtt.**
-dontwarn com.itextpdf.text.pdf.security.**
-dontwarn com.itextpdf.text.pdf.PdfEncryptor
-dontwarn com.itextpdf.text.pdf.PdfReader
-dontwarn com.itextpdf.text.pdf.PdfPublicKeySecurityHandler
-dontwarn java.sql.**
-dontwarn javax.xml.crypto.**
-dontwarn com.sun.javafx.**
-dontwarn jdk.jfr.**
