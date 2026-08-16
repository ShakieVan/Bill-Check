-keep class * extends androidx.room.RoomDatabase { *; }

# R8 8.x can horizontally merge ML Kit's generated lazy-instance and telemetry
# classes with unrelated generated classes. On Android 16 this produced a null
# component during InputImage creation in the minified release build. Keep the
# bundled OCR implementation intact; debug builds are unaffected either way.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
