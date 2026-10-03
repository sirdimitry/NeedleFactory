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
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "skipped", "failed") }
}
