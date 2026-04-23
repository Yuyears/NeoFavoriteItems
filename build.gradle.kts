plugins {
    java
    idea
    eclipse
    id("dev.architectury.loom") version "1.13.469" apply false
    id("architectury-plugin") version "3.4.162" apply false
}

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
            archiveFileName.set("${rootProject.property("archives_base_name")}-${project.name}-${project.version}-${rootProject.property("build_number")}.jar")
        }
    }

}
