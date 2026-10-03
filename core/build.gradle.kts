import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin { compilerOptions.jvmTarget.set(JvmTarget.JVM_11) }

dependencies {
    api(libs.gdx)
    api(libs.gdx.box2d)
    testRuntimeOnly("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-desktop")
    testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-desktop")
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "skipped", "failed") }
}
