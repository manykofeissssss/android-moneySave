pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "BillKeeper"
include(":app")

includeBuild("D:/CreateByCodex/2026-08-24/SharedNav")
 {
    dependencySubstitution {
        substitute(module("com.xah.sharednav:navigation"))
            .using(project(":navigation"))
    }
}

includeBuild("D:/CreateByCodex/2026-09-12/k-diagnostics") {
    dependencySubstitution {
        substitute(module("io.github.manykofeissssss.kdiagnostics:diagnostics-android"))
            .using(project(":diagnostics-android"))
        substitute(module("io.github.manykofeissssss.kdiagnostics:diagnostics-work"))
            .using(project(":diagnostics-work"))
    }
}
