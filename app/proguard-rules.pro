# Keine Minifizierung aktiv. Regeln für kotlinx.serialization, falls aktiviert:
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.planora.app.**$$serializer { *; }
-keepclassmembers class com.planora.app.** { *** Companion; }
