// Top-level build file
plugins {
    // Dal 03/11/2026 (verso PageFlow per Windows e Mac): Kotlin 2, il
    // compilatore di Compose che ne fa parte, KSP 2 e l'ultimo plugin Android 8.
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
    // Dal 04/11/2026: il codice comune (`shared`) e l'app per il PC (`desktop`).
    // Compose Multiplatform 1.11 è lo stesso Compose 1.11 dell'app Android.
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.11.1" apply false
}
