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

# Preservar las clases internas de Kotlin que Sora Editor necesita
# ShareableData usa Cloneable con implementaciones por defecto
-keep class kotlin.Cloneable { *; }
-keep class kotlin.Cloneable$DefaultImpls { *; }
-keep class kotlin.jvm.internal.** { *; }

# Preservar todas las interfaces de Sora que pueden tener DefaultImpls
-keep interface io.github.rosemoe.sora.** { *; }

# Preservar cualquier clase DefaultImpls generada por Kotlin
-keep class **$DefaultImpls { *; }

# No advertir sobre clases de Kotlin que R8 no puede resolver
-dontwarn kotlin.Cloneable$DefaultImpls
-dontwarn kotlin.jvm.internal.**

# ==========================================
# Reglas para Zip4j
# ==========================================
-keep class net.lingala.zip4j.** { *; }