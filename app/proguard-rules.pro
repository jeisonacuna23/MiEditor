# Reglas para sora-editor (evita que R8 elimine el motor TextMate)
-keep class io.github.rosemoe.sora.langs.textmate.** { *; }
-keep class org.eclipse.tm4e.** { *; }
-keep class org.joni.** { *; }
-keep class org.jcodings.** { *; }

# Evitar que R8 se queje por clases faltantes de la librería
-dontwarn io.github.rosemoe.sora.**
-dontwarn org.eclipse.tm4e.**
