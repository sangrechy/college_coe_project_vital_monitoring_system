# Keep Kotlin metadata and coroutines intact
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**
