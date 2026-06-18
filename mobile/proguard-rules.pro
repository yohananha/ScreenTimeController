# Keep rules for the mobile release build. See tv/proguard-rules.pro for
# rationale — the libraries we use ship consumer rules; we cover only the
# pieces R8 can't infer.

# Application + Hilt-generated entry point — referenced by string in the
# manifest.
-keep class com.screentime.mobile.MobileApp { *; }
-keep class com.screentime.mobile.Hilt_MobileApp { *; }

# FCM service — declared in the manifest, instantiated by the system.
-keep class com.screentime.mobile.fcm.PushService { *; }

# Our data models — accessed reflectively by name through Firestore field
# lookups.
-keep class com.screentime.shared.model.** { *; }
-keep class com.screentime.shared.room.** { *; }

# Keep @Inject constructors so Hilt-generated factory code keeps working.
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions

# SQLCipher loads native code; keep the JNI surface.
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { native <methods>; }

# Crashlytics: preserve unobfuscated line numbers in production stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Credentials API uses reflection on response payload types.
-keep class com.google.android.libraries.identity.googleid.** { *; }
