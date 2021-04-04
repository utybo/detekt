import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    kotlin("jvm") apply false
    jacoco
    packaging
    releasing
    detekt
    id("com.github.ben-manes.versions")
    id("org.sonarqube")
}

allprojects {
    group = "io.gitlab.arturbosch.detekt"
    version = Versions.currentOrSnapshot()
}

jacoco.toolVersion = Versions.JACOCO

dependencies {
    implementation(project("custom-checks"))
    implementation(project("detekt-api"))
    implementation(project("detekt-cli"))
    implementation(project("detekt-core"))
    implementation(project("detekt-formatting"))
    implementation(project("detekt-generator"))
    implementation(project("detekt-gradle-plugin"))
    implementation(project("detekt-metrics"))
    implementation(project("detekt-parser"))
    implementation(project("detekt-psi-utils"))
    implementation(project("detekt-report-html"))
    implementation(project("detekt-report-sarif"))
    implementation(project("detekt-report-txt"))
    implementation(project("detekt-report-xml"))
    implementation(project("detekt-rules"))
    implementation(project("detekt-rules-complexity"))
    implementation(project("detekt-rules-coroutines"))
    implementation(project("detekt-rules-documentation"))
    implementation(project("detekt-rules-empty"))
    implementation(project("detekt-rules-errorprone"))
    implementation(project("detekt-rules-exceptions"))
    implementation(project("detekt-rules-naming"))
    implementation(project("detekt-rules-performance"))
    implementation(project("detekt-rules-style"))
    implementation(project("detekt-tooling"))
}

tasks {
    jacocoTestReport {
        executionData.setFrom(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))
        val examplesOrTestUtils = setOf(
            "detekt-bom",
            "detekt-test",
            "detekt-test-utils",
            "detekt-sample-extensions"
        )
        subprojects
            .filterNot { it.name in examplesOrTestUtils }
            .forEach {
                this@jacocoTestReport.sourceSets(it.sourceSets.main.get())
                this@jacocoTestReport.dependsOn(it.tasks.test)
            }
        reports {
            xml.isEnabled = true
            xml.destination = file("$buildDir/reports/jacoco/report.xml")
        }
    }
}

// A resolvable configuration to collect source code
val jacocoSourceDirs: Configuration by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("source-folders"))
    }
}

// A resolvable configuration to collect JaCoCo coverage data
val jacocoExecutionData: Configuration by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("jacoco-coverage-data"))
    }
}

val jacocoClassDirs: Configuration by configurations.creating {
    extendsFrom(configurations.implementation.get())
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
    }
}

val jacocoMergedReport by tasks.registering(JacocoReport::class) {
    executionData.from(jacocoExecutionData.incoming.artifacts.artifactFiles)
    sourceDirectories.from(jacocoSourceDirs.incoming.artifacts.artifactFiles)
    classDirectories.from(jacocoClassDirs.incoming.artifacts.artifactFiles)

    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

val analysisDir = file(projectDir)
val baselineFile = file("$rootDir/config/detekt/baseline.xml")
val configFile = file("$rootDir/config/detekt/detekt.yml")
val statisticsConfigFile = file("$rootDir/config/detekt/statistics.yml")

val kotlinFiles = "**/*.kt"
val kotlinScriptFiles = "**/*.kts"
val resourceFiles = "**/resources/**"
val buildFiles = "**/build/**"

val detektFormat by tasks.registering(Detekt::class) {
    description = "Formats whole project."
    parallel = true
    disableDefaultRuleSets = true
    buildUponDefaultConfig = true
    autoCorrect = true
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
    reports {
        xml.enabled = false
        html.enabled = false
        txt.enabled = false
    }
}

val detektAll by tasks.registering(Detekt::class) {
    description = "Runs the whole project at once."
    parallel = true
    buildUponDefaultConfig = true
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
    reports {
        xml.enabled = false
        html.enabled = false
        txt.enabled = false
    }
}

val detektProjectBaseline by tasks.registering(DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
}
