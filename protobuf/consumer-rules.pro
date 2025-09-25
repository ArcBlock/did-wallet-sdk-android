# Protobuf rules for R8/ProGuard
# Keep all protobuf classes
-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * implements com.google.protobuf.MessageLiteOrBuilder { *; }

# Keep protobuf enum classes
-keep class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Keep all generated protobuf classes in our packages
-keep class ocap.** { *; }
-keep class vendor.** { *; }
-keep class io.arcblock.protobuf.** { *; }

# Keep protobuf methods used by reflection
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}

# Keep protobuf builder classes
-keep class * extends com.google.protobuf.GeneratedMessageLite$Builder { *; }

# Suppress warnings for protobuf
-dontwarn com.google.protobuf.**