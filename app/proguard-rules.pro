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

# Preservar las clases DefaultImpls generadas por Kotlin para interfaces
# Sora Editor usa ShareableData que hereda de Cloneable con implementaciones por defecto
-keep class kotlin.Cloneable$DefaultImpls { *; }
-keep class io.github.rosemoe.sora.util.ShareableData$DefaultImpls { *; }
-keep interface io.github.rosemoe.sora.util.ShareableData { *; }

# Preservar cualquier clase que implemente Cloneable (necesario para el clone() en DefaultImpls)
-keep class * implements kotlin.Cloneable { *; }

# Preservar todas las clases DefaultImpls de Kotlin (generadas para interfaces con métodos por defecto)
-keep class **$DefaultImpls { *; }

# ==========================================
# Reglas para Zip4j
# ==========================================
-keep class net.lingala.zip4j.** { *; }