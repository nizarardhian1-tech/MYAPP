# ProGuard rules untuk com.tes.aoo

# Pertahankan semua class milik paket ini
-keep class com.tes.aoo.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Gson
-keep class com.google.gson.** { *; }
