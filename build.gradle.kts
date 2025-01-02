// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin) apply false
    alias(libs.plugins.jetbrains.dokka) apply true
}

buildscript {
    repositories {
        //google()
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
    dependencies {
        //classpath("com.github.pedroSG94.RootEncoder:library:2.5.5")
        //Optional, allow use BitmapSource, CameraXSource and CameraUvcSource
//        classpath("com.github.pedroSG94.RootEncoder:extra-sources:2.5.5")
    }
}

allprojects {
    repositories {
//        google()
//        jcenter()
//        mavenCentral()
//        gradlePluginPortal()

//        maven {
//            url = uri("https://jitpack.io")
//        }
//        maven {
//            url  = uri("https://maven.google.com")
//        }
    }
}

tasks.register("clean") {
    delete(layout.buildDirectory)
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(File("docs"))
}
