import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val natives by configurations.creating
val nativeOutput = layout.buildDirectory.dir("generated/gdx-natives")
val copyAndroidNatives by tasks.registering {
    inputs.files(natives)
    outputs.dir(nativeOutput)
    doLast {
        sync {
            into(nativeOutput)
            listOf("armeabi-v7a", "arm64-v8a", "x86_64").forEach { abi ->
                into(abi) {
                    from(natives.files.filter { it.name.endsWith("natives-$abi.jar") }.map { zipTree(it) })
                    include("*.so")
                }
            }
        }
    }
}

android {
    namespace = "com.needlefactory.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.needlefactory.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = project.version.toString()
    }
    buildFeatures { buildConfig = true }
    sourceSets["main"].apply {
        assets.srcDir(rootProject.file("assets"))
        assets.srcDir(rootProject.file("core/src/main/resources"))
        jniLibs.srcDir(nativeOutput)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}

kotlin { compilerOptions.jvmTarget.set(JvmTarget.JVM_11) }

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.android)
    listOf("armeabi-v7a", "arm64-v8a", "x86_64").forEach { abi ->
        natives("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-$abi")
        natives("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-$abi")
    }
}

tasks.named("preBuild") { dependsOn(copyAndroidNatives) }
