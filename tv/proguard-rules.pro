# Keep rules for the TV release build.
#
# Hilt, Firebase, Room, Compose, WorkManager, Crashlytics and SQLCipher all
# ship their own consumer rules; we add only the items that R8 cannot infer
# from bytecode + AGP's standard analysis.

# Application + Hilt-generated entry point — referenced from the manifest by
# string, so R8 can't see the link without help.
-keep class com.screentime.tv.ScreenTimeTvApp { *; }
-keep class com.screentime.tv.Hilt_ScreenTimeTvApp { *; }

# Our data models — accessed reflectively by name through Firestore field
# lookups. We don't use toObject() but keeping the class names avoids
# stack traces becoming useless after a crash.
-keep class com.screentime.shared.model.** { *; }
-keep class com.screentime.shared.room.** { *; }

# Keep @Inject constructors so Hilt-generated factory code keeps working.
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Coroutine internals reference debug metadata via reflection.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions

# SQLCipher loads the native library by name and uses JNI hooks.
-keep class net.zetetic.database.sqlcipher.** { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.** { native <methods>; }

# Crashlytics: preserve unobfuscated line numbers in production stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
