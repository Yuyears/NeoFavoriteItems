plugins {
    java
    idea
    eclipse
    id("dev.architectury.loom") version "1.13.469" apply false
    id("architectury-plugin") version "3.4.162" apply false
}

fun shouldIncrementBuildNumber(): Boolean {
    if ((findProperty("skip_build_number_increment") as String?)?.toBoolean() == true) {
        return false
    }
    if (gradle.startParameter.isDryRun) {
        return false
    }

    return gradle.startParameter.taskNames
        .map { it.substringAfterLast(':').lowercase() }
        .any { taskName ->
            taskName in setOf("build", "assemble", "jar", "remapjar")
        }
}

fun incrementBuildNumberIfNeeded(): String {
    val currentBuildNumber = property("build_number") as String
    val currentModVersion = property("mod_version") as String
    if (!shouldIncrementBuildNumber()) {
        return currentBuildNumber
    }

    val propertiesFile = rootProject.file("gradle.properties")
    val propertiesText = propertiesFile.readText()
    val buildNumberRegex = Regex("""(?m)^build_number=(build)?(\d+)\s*$""")
    val match = buildNumberRegex.find(propertiesText)
        ?: error("Missing build_number entry in gradle.properties")
    val buildNumberVersionRegex = Regex("""(?m)^build_number_version=(.+?)\s*$""")
    val versionMatch = buildNumberVersionRegex.find(propertiesText)
    val previousBuildNumberVersion = versionMatch?.groupValues?.get(1)

    val prefix = match.groupValues[1].ifEmpty { "build" }
    val versionChanged = previousBuildNumberVersion != currentModVersion
    val nextNumber = if (versionChanged) 1 else match.groupValues[2].toInt() + 1
    val nextBuildNumber = "$prefix$nextNumber"
    val versionedPropertiesText = if (versionMatch == null) {
        propertiesText.replace(
            buildNumberRegex,
            "build_number_version=$currentModVersion${System.lineSeparator()}build_number=$nextBuildNumber"
        )
    } else {
        buildNumberVersionRegex.replace(
            buildNumberRegex.replace(propertiesText, "build_number=$nextBuildNumber"),
            "build_number_version=$currentModVersion"
        )
    }
    propertiesFile.writeText(versionedPropertiesText)
    if (versionChanged) {
        logger.lifecycle("Reset build_number for version change: $previousBuildNumberVersion -> $currentModVersion; $currentBuildNumber -> $nextBuildNumber")
    } else {
        logger.lifecycle("Incremented build_number: $currentBuildNumber -> $nextBuildNumber")
    }
    return nextBuildNumber
}

val resolvedBuildNumber = incrementBuildNumberIfNeeded()
extra["build_number"] = resolvedBuildNumber

allprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "eclipse")

    group = property("maven_group") as String
    version = property("mod_version") as String

    repositories {
        mavenCentral()
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        if (project.name in setOf("fabric", "forge", "neoforge")) {
            archiveFileName.set("${rootProject.property("archives_base_name")}-${project.name}-${project.version}-${resolvedBuildNumber}.jar")
        }
    }

}

val loaderProjectNames = listOf("fabric", "forge", "neoforge")
val copyLoaderJarsToResult = tasks.register<Copy>("copyLoaderJarsToResult") {
    group = "build"
    description = "Copies Fabric, Forge, and NeoForge release jars into build/result."

    dependsOn(loaderProjectNames.map { ":$it:remapJar" })
    into(layout.buildDirectory.dir("result"))

    doFirst {
        delete(layout.buildDirectory.dir("result"))
    }

    loaderProjectNames.forEach { loaderName ->
        from(project(":$loaderName").layout.buildDirectory.dir("libs")) {
            include("${rootProject.property("archives_base_name")}-$loaderName-${rootProject.property("mod_version")}-$resolvedBuildNumber.jar")
        }
    }
}

tasks.named("build") {
    dependsOn(copyLoaderJarsToResult)
}

gradle.projectsEvaluated {
    loaderProjectNames.forEach { loaderName ->
        project(":$loaderName").tasks.findByName("remapJar")?.finalizedBy(copyLoaderJarsToResult)
    }
}
