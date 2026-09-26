plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    // Robolectric legge il manifest unito, che contiene l'attività vuota
    // su cui i test disegnano le schermate (ui-test-manifest).
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    namespace = "com.gabriele.notionlocal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gabriele.notionlocal"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android + Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Legge l'orientamento scritto dentro le foto (EXIF). Serve alle
    // copertine: le foto scattate col telefono ruotato sono salvate
    // "dritte" con dentro un'etichetta che dice di quanto girarle, e
    // chi non la legge le mostra coricate.
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // I font cinesi e giapponesi delle pagine, scaricati da Google Play
    // Services la prima volta che servono (pesano 10-20 MB l'uno: dentro
    // l'app la farebbero crescere di oltre cento). Vedi `PageFonts.kt`.
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Navigation tra le schermate
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel + Compose integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    // collectAsStateWithLifecycle: raccoglie i Flow rispettando il ciclo
    // di vita della schermata (si ferma quando l'app va in background)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Room (database SQLite locale) - il cuore del "tutto salvato sul telefono"
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines per operazioni asincrone sul database
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Serializzazione JSON per il rich text (RichTextSpan) e le opzioni SELECT.
    // Fissata a 1.6.3: dalla 1.7.0 in poi la libreria richiede Kotlin 2.0+,
    // mentre questo progetto usa Kotlin 1.9.24 in tutta la configurazione
    // (Compose compiler, KSP...). Nessuna funzione usata nel codice richiede
    // una versione più recente.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Test
    testImplementation("junit:junit:4.13.2")
    // Test del repository su un database vero, senza telefono: Robolectric
    // fa girare Android (e quindi SQLite e Room) sulla JVM. Aggiunti il
    // 24/09/2026 per provare in cloud le operazioni che toccano i dati
    // (spostare, duplicare, annullare), che lì non si possono provare
    // sul telefono. Solo per i test: nell'app non entrano.
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
    // Per disegnare le schermate in un test e guardarle come immagine
    // (i widget della barra laterale, 24/09/2026): in cloud il telefono
    // non c'è, e questo è l'unico modo di vederle.
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
