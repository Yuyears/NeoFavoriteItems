plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

configurations.configureEach {
    exclude(group = "net.fabricmc", module = "fabric-log4j-util")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
    })

    add("neoForge", "net.neoforged:neoforge:${property("neoforge_version")}")
}

sourceSets.main {
    java.srcDirs(project(":common").sourceSets.main.get().java.srcDirs)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
    
    // 包含 common 模块的资源文件
    from(project(":common").sourceSets.main.get().resources.srcDirs)
}
