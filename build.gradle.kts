import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()
}

val staticAnalysisExcludes = listOf(
    "**/build/**",
    "**/.gradle/**",
)

val ktlintEditorConfig = mapOf(
    "ktlint_code_style" to "android_studio",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    "max_line_length" to "140",
)
val ktlintToolVersion = libs.versions.ktlint.get()

allprojects {
    apply(plugin = "dev.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        basePath.set(rootDir)
    }

    extensions.configure<KtlintExtension> {
        android.set(true)
        additionalEditorconfig.set(ktlintEditorConfig)
        filter {
            exclude(staticAnalysisExcludes)
        }
    }

    extensions.configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude(staticAnalysisExcludes)
            ktlint(ktlintToolVersion).editorConfigOverride(ktlintEditorConfig)
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude(staticAnalysisExcludes)
            ktlint(ktlintToolVersion).editorConfigOverride(ktlintEditorConfig)
        }
        format("misc") {
            target("**/*.md", "**/*.yml", "**/*.yaml")
            targetExclude(staticAnalysisExcludes)
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.withType<Detekt>().configureEach {
        exclude(staticAnalysisExcludes)
    }
}

fun tasksNamedInProjects(name: String) = allprojects.map { project ->
    project.tasks.matching { task -> task.name == name }
}

tasks.register("staticAnalysis") {
    group = "verification"
    description = "Runs detekt, ktlint, and Spotless checks."
    dependsOn(
        tasksNamedInProjects("detekt"),
        tasksNamedInProjects("ktlintCheck"),
        tasksNamedInProjects("spotlessCheck"),
    )
}

tasks.register("checkAll") {
    group = "verification"
    description = "Runs tests, API validation, Android lint, static analysis, and local Maven publishing."
    dependsOn(
        "staticAnalysis",
        tasksNamedInProjects("test"),
        tasksNamedInProjects("apiCheck"),
        tasksNamedInProjects("lint"),
        tasksNamedInProjects("publishToMavenLocal"),
    )
}

apiValidation {
    ignoredProjects.addAll(
        listOf(
            "sample",
            "macrobenchmark",
        ),
    )

    ignoredClasses.add("com.rend1x.composeanimatedlist.BuildConfig")
}
