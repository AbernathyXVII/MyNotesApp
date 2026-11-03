// Top-level build file
plugins {
    // Dal 03/11/2026 (verso PageFlow per Windows e Mac): Kotlin 2, il
    // compilatore di Compose che ne fa parte, KSP 2 e l'ultimo plugin Android 8.
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}
