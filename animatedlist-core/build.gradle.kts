plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
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

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = providers.gradleProperty("GROUP").get(),
        artifactId = "${providers.gradleProperty("POM_ARTIFACT_ID").get()}-core",
        version = providers.gradleProperty("VERSION_NAME").get(),
    )

    pom {
        name.set("Compose Animated List Core")
        description.set("JVM-only diff and render engine for Compose Animated List.")
        inceptionYear.set("2026")
        url.set(providers.gradleProperty("PROJECT_URL").get())

        licenses {
            license {
                name.set(providers.gradleProperty("PROJECT_LICENSE_NAME").get())
                url.set(providers.gradleProperty("PROJECT_LICENSE_URL").get())
                distribution.set(providers.gradleProperty("PROJECT_LICENSE_DIST").get())
            }
        }

        developers {
            developer {
                id.set(providers.gradleProperty("PROJECT_DEVELOPER_ID").get())
                name.set(providers.gradleProperty("PROJECT_DEVELOPER_NAME").get())
                url.set(providers.gradleProperty("PROJECT_DEVELOPER_URL").get())
            }
        }

        scm {
            url.set(providers.gradleProperty("PROJECT_SCM_URL").get())
            connection.set(providers.gradleProperty("PROJECT_SCM_CONNECTION").get())
            developerConnection.set(providers.gradleProperty("PROJECT_SCM_DEV_CONNECTION").get())
        }
    }
}
