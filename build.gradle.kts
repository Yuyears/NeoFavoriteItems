plugins {
    java
    idea
    eclipse
    id("dev.architectury.loom") version "1.13.469" apply false
    id("architectury-plugin") version "3.4.162" apply false
}

val resolvedBuildNumber = providers.gradleProperty("build_number").orElse("build0").get()
val releaseChannel = providers.gradleProperty("release_channel").orElse("dev").get()
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
            archiveFileName.set("${rootProject.property("archives_base_name")}-${rootProject.property("minecraft_version")}-${project.name}-${releaseChannel}-${project.version}-${resolvedBuildNumber}.jar")
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
            include("${rootProject.property("archives_base_name")}-${rootProject.property("minecraft_version")}-$loaderName-$releaseChannel-${rootProject.property("mod_version")}-$resolvedBuildNumber.jar")
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
