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

tasks.register("releaseCheck") {
    group = "verification"
    description = "Runs release verification: tests, compilation, API checks, lint, and static analysis."
    dependsOn(
        "staticAnalysis",
        tasksNamedInProjects("test"),
        tasksNamedInProjects("apiCheck"),
        tasksNamedInProjects("lint"),
        ":animatedlist-core:compileKotlin",
        ":animatedlist:compileReleaseKotlin",
        ":sample:compileDebugKotlin",
    )
}

tasks.register("releaseTag") {
    group = "publishing"
    description = "Runs checks, creates an annotated release tag from VERSION_NAME, and optionally pushes it."

    dependsOn("releaseCheck")

    doLast {
        val versionName = providers.gradleProperty("VERSION_NAME").get()
        val tagName = "v$versionName"
        val releaseBranchName = "release/$versionName"
        val shouldPush = providers.gradleProperty("release.push")
            .map(String::toBoolean)
            .getOrElse(false)

        fun gitOutput(vararg args: String): String = providers.exec {
            commandLine("git", *args)
        }.standardOutput.asText.get().trim()

        fun gitExit(vararg args: String): Int = providers.exec {
            isIgnoreExitValue = true
            commandLine("git", *args)
        }.result.get().exitValue

        fun git(vararg args: String) {
            providers.exec {
                commandLine("git", *args)
            }.result.get().assertNormalExitValue()
        }

        val status = gitOutput("status", "--porcelain")
        check(status.isBlank()) {
            "Working tree is not clean. Commit or stash changes before creating a release tag."
        }

        val currentBranch = gitOutput("branch", "--show-current")
        check(currentBranch == releaseBranchName) {
            "Release tag must be created from $releaseBranchName. Current branch is $currentBranch."
        }

        val tagExists = gitExit("rev-parse", "-q", "--verify", "refs/tags/$tagName") == 0
        check(!tagExists) {
            "Tag $tagName already exists."
        }

        git("tag", "-a", tagName, "-m", "Release $tagName")

        if (shouldPush) {
            git("push", "origin", "HEAD:refs/heads/$releaseBranchName")
            git("push", "origin", tagName)
        } else {
            logger.lifecycle("Created tag $tagName locally.")
            logger.lifecycle("Push the release branch with: git push origin HEAD:refs/heads/$releaseBranchName")
            logger.lifecycle("Push it with: git push origin $tagName")
            logger.lifecycle("Or run: ./gradlew releaseTag -Prelease.push=true")
        }
    }
}

apiValidation {
    ignoredProjects.addAll(
        listOf(
            "sample",
            "macrobenchmark",
            "animatedlist-lint",
        ),
    )

    ignoredClasses.add("com.rend1x.composeanimatedlist.BuildConfig")
}
