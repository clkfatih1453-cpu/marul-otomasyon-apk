# Eclipse Paho MQTT
-keep class org.eclipse.paho.** { *; }
-keepclassmembers class org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.**

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

# OkHttp (kullanılmıyor, kaldırıldı)
# -dontwarn okhttp3.**
# -dontwarn okio.**
# -keep class okhttp3.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
