import java.net.URI

pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.myket.ir")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://maven.myket.ir")
        }
        google()
        mavenCentral()
        maven { url = URI("https://jitpack.io") }
    }
}

rootProject.name = "AutoClicker"
include(":app")
 