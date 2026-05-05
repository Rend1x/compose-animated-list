plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.android.lint.api)
    compileOnly(libs.android.lint.checks)

    testImplementation(libs.android.lint.api)
    testImplementation(libs.android.lint.checks)
    testImplementation(libs.android.lint.tests)
    testImplementation(libs.junit)
}

configurations.named("runtimeElements") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
}

tasks.jar {
    manifest {
        attributes("Lint-Registry-v2" to "com.rend1x.composeanimatedlist.lint.ComposeAnimatedListIssueRegistry")
    }
}
