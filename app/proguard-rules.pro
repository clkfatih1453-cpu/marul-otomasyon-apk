# Blynk
-keep class com.blynk.** { *; }
-keepclassmembers class com.blynk.** { *; }
-dontwarn com.blynk.**

# Kotlin
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.** { *; }
-dontwarn kotlin.**

# AndroidX
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }
-dontwarn androidx.**

# Marul Otomasyon
-keep class com.marul.otomasyon.** { *; }
-keepclassmembers class com.marul.otomasyon.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
