import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}
group = "io.github.stephenwanjala"
version = "0.0.2"

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "komposetable", version.toString())

    pom {
        name = "Table Componet For Compose UI"
        description =
            "A highly customizable table component with A nealy Similar API as JavaFx TableView for Compose Multiplatform, offering features like sorting, column resizing, row selection, and theming. "
        inceptionYear = "2025"
        url = " https://github.com/stephenWanjala/KomposeTable/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "komposetable"
                name = "Wanjala Stephen"
                url = "github.com/stephenWanjala/"
            }
        }
        scm {
            url = " https://github.com/stephenWanjala/KomposeTable/"
            connection = "scm:git:git:/github.com/stephenWanjala/KomposeTable.git"
            developerConnection = "scm:git:ssh://git@github.com/stephenWanjala/KomposeTable.git"
        }
    }
}



kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()


    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(libs.material3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.github.stephenwanjala.komposetable"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
