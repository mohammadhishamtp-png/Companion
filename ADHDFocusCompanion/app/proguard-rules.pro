# Keep Gemini/Retrofit model classes (Gson reflection)
-keep class com.adhdcompanion.focus.data.remote.** { *; }
-keep class com.adhdcompanion.focus.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
