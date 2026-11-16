# Production ProGuard / R8 rules for PlaySafe Audit

# Preserve source file & line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Keep domain models & Moshi codegen adapters
-keep class com.example.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}

# Keep Room database, entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Suppress benign warnings from optional Moshi Kotlin reflect fallback
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn org.jetbrains.annotations.**
