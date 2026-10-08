# ==========================================
# Reglas para Kotlinx Serialization
# ==========================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.json.** Companion;
}

-keep,includedescriptorclasses class com.twedmark.app.**$$serializer { *; }
-keepclassmembers class com.twedmark.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.twedmark.app.** {
    kotlinx.serialization.KSerializer serializer(...);
    kotlinx.serialization.KSerializer serializer();
}

# ==========================================
# Reglas para Sora Editor y TextMate
# ==========================================
-keep class io.github.rosemoe.sora.** { *; }
-keep class org.eclipse.tm4e.** { *; }

# ==========================================
# Reglas para Zip4j
# ==========================================
-keep class net.lingala.zip4j.** { *; }