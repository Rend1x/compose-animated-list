plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.rend1x.composeanimatedlist"
    compileSdk = 36

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":animatedlist-core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    debugImplementation(libs.androidx.compose.material)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)

    testImplementation(libs.junit)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = providers.gradleProperty("GROUP").get(),
        artifactId = providers.gradleProperty("POM_ARTIFACT_ID").get(),
        version = providers.gradleProperty("VERSION_NAME").get(),
    )

    pom {
        name.set("Compose Animated List")
        description.set("Animated list utilities for Jetpack Compose.")
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
