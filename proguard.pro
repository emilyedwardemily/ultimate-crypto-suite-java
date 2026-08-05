#
# ULTIMATE CRYPTO SUITE - ProGuard obfuscation config
#
# Target: JavaFX desktop app, classpath build (no module-info).
# Note: requires proguard-core >= 9.2.0 to parse Java 25 (class version 69)
#       bytecode. See pom.xml for the dependency override.
#

# --- Basic tuning -----------------------------------------------------------
# NOTE: no -target backport flag. ProGuard 7.7.0 can only backport to <= Java 11,
# and our input classes are Java 25 (version 69); backporting is disabled so the
# original class-file versions are preserved.
-optimizationpasses 3
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively

# Keep line number info mapped to a file for future troubleshooting.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Entry points (must survive obfuscation) --------------------------------
# JavaFX launcher + application class (reflected by Application.launch).
-keep public class app.Launcher {
    public static void main(java.lang.String[]);
}
-keep class app.MainApp { *; }

# Keep the integrity guard fully intact so the anti-tamper logic is not
# removed or renamed in a way that breaks verification.
-keep class app.TamperGuard { *; }
-keep,allowobfuscation class app.TamperGuard { <fields>; }

# --- Libraries / reflection / serialization ---------------------------------
# org.json (JSONObject/JSONArray via reflection into Java records/beans).
-keep class org.json.** { *; }

# Gson uses reflection heavily.
-keep,allowshrinking class com.google.gson.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# BouncyCastle is reflection-driven (JCE providers).
-keep class org.bouncycastle.** { *; }

# MongoDB sync driver relies on class metadata.
-keep class com.mongodb.** { *; }
-keep class org.bson.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }

# iText5 uses reflection for page events / font handling.
-keep class com.itextpdf.** { *; }
-keep class com.lowagie.** { *; }

# ZXing QR writer accessed from Dashboard (reflection of result types).
-keep class com.google.zxing.** { *; }

# --- Enums / annotations / UI ------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepattributes *Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod
-keep @javax.annotation.* class *

# JavaFX: keep public API used by FXML loader + controller wiring.
-keep class * extends javafx.application.Application { *; }
-keepclassmembers class * extends javafx.scene.control.* {
    <init>(...);
}
-dontwarn javax.annotation.**
-dontwarn javafx.**
-dontwarn org.controlsfx.**

# --- Optional string encryption (endpoints/API keys/headers) -----------------
# AppConfig centralizes secrets; keep the class and obfuscate its content.
-keep class app.AppConfig { *; }

# --- Shrink safe: keep all app classes reachable from entry points -----------
-keep public class * {
    public protected *;
}

# --- Dump / debug output ----------------------------------------------------
-dontnote **
-verbose
-printmapping obfuscation-map.txt

# --- Optional / conditional dependencies (not shipped) -----------------------
# MongoDB sync driver: netty transport + client-side field-level encryption are
# optional modules not present in the distribution.
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
# iText 5: PKCS#7 signing / OCSP / TSP require bcpkix,bctsp,bcmail (not shipped).
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
# Gson's java.sql adapters (module java.sql not linked into the runtime image).
-dontwarn java.sql.**
-dontwarn javax.xml.crypto.**
-dontwarn com.sun.javafx.**
-dontwarn jdk.jfr.**
