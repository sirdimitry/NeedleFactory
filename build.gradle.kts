plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
}

allprojects {
    group = "com.needlefactory"
    version = "0.0.1"
}
