plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

architectury {
    platformSetupLoomIde()
    forge()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
    })

    add("forge", "net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    implementation(project(":common"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
    
    // 包含 common 模块的资源文件
    from(project(":common").sourceSets.main.get().resources.srcDirs)
}
