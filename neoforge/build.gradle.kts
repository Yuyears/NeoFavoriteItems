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
    // 排除 Fabric API，避免编译时找不到 EnvType 类的警告
    exclude(group = "net.fabricmc.fabric-api")
    exclude(group = "net.fabricmc", module = "fabric-loader")
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

// 设置 Java 编译编码为 UTF-8，避免中文注释乱码
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
