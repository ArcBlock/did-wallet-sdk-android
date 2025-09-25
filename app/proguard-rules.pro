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

# Protobuf rules for R8/ProGuard - Comprehensive set
# Keep all protobuf core classes
-keep class com.google.protobuf.** { *; }
-keep interface com.google.protobuf.** { *; }
-keep enum com.google.protobuf.** { *; }

# Keep protobuf lite classes
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * implements com.google.protobuf.MessageLiteOrBuilder { *; }
-keep class * implements com.google.protobuf.Internal$EnumLite { *; }

# Keep protobuf enum classes - comprehensive rules
-keep class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Specific rules for protobuf enums implementing EnumLite
-keep class * extends java.lang.Enum,
             * implements com.google.protobuf.Internal$EnumLite {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** forNumber(int);
    public int getNumber();
    **[] $VALUES;
    public *;
}

# Keep all generated protobuf classes in our packages
-keep class ocap.** { *; }
-keep class vendor.** { *; }
-keep class io.arcblock.protobuf.** { *; }

# Keep nested classes and inner enums
-keep class ocap.Enum$* { *; }
-keep class vendor.Vendor$* { *; }

# Keep protobuf methods used by reflection
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}

# Keep protobuf builder classes
-keep class * extends com.google.protobuf.GeneratedMessageLite$Builder { *; }

# Keep Internal classes that R8 might optimize away
-keep class com.google.protobuf.Internal { *; }
-keep class com.google.protobuf.Internal$* { *; }

# Additional protobuf rules for method references
-keepclassmembers class * implements com.google.protobuf.Internal$EnumLite {
    public int getNumber();
}

# Suppress warnings for protobuf
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**