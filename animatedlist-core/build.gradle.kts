plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}

tasks.register<JavaExec>("runBenchmarks") {
    group = "verification"
    description = "Runs repeatable animatedlist-core benchmark scenarios."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.rend1x.composeanimatedlist.core.AnimatedListBenchmarkRunnerKt")
    jvmArgs("-Xms512m", "-Xmx512m")
}
