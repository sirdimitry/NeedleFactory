import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin { compilerOptions.jvmTarget.set(JvmTarget.JVM_11) }

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.lwjgl3)
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-desktop")
    runtimeOnly("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-desktop")
}

application { mainClass.set("com.needlefactory.lwjgl3.DesktopLauncher") }

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
    // Release-style diagnostics check: ./gradlew :lwjgl3:run -Pdiagnostics=false
    systemProperty("needlefactory.diagnostics", providers.gradleProperty("diagnostics").getOrElse("true"))
    systemProperty("needlefactory.seed", providers.gradleProperty("seed").getOrElse(""))
    systemProperty("needlefactory.screen", providers.gradleProperty("screen").getOrElse("factory"))
}

// Optional real OpenGL verification. Not part of headless JVM tests or CI.
tasks.register<JavaExec>("audioMenuSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.AudioMenuSmoke")
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("equipmentSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.EquipmentSmoke")
    workingDir = rootProject.projectDir
}
tasks.register<JavaExec>("pitSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.PitSmoke")
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("transportSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.TransportSmoke")
    workingDir = rootProject.projectDir
    systemProperty("needlefactory.transport.preview",providers.gradleProperty("transportPreview").getOrElse(""))
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("grabSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.GrabSmoke")
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("productSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.ProductSmoke")
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("diagnosticSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.DiagnosticSmoke")
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.register<JavaExec>("prototypeSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.PrototypeSmoke")
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.register<JavaExec>("factorySmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.FactorySmoke")
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) jvmArgs("-XstartOnFirstThread")
}

tasks.register<JavaExec>("workshopSmoke") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.needlefactory.lwjgl3.WorkshopSmoke")
    workingDir = rootProject.projectDir
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) jvmArgs("-XstartOnFirstThread")
}
