pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PageFlow"
// **Tre moduli** (dal 04/11/2026, verso Windows e Mac): `shared` è il codice
// comune al telefono e al PC (Kotlin Multiplatform), `app` l'app Android,
// `desktop` quella per Windows e Mac (Compose Multiplatform).
include(":app", ":shared", ":desktop")
