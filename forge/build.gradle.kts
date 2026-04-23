plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

architectury {
    platformSetupLoomIde()
    compileOnly()
    forge()
}

configurations.configureEach {
    exclude(group = "net.fabricmc", module = "fabric-log4j-util")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
    })

    add("forge", "net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
}

loom {
    runs {
        named("client") {
            vmArg("-Dearlydisplay.disable=true")
            // 强制禁用早期显示窗口
            property("fml.earlyprogresswindow", "false")
        }
        named("server") {
            vmArg("-Dearlydisplay.disable=true")
            property("fml.earlyprogresswindow", "false")
        }
    }
}

sourceSets.main {
    java.srcDirs(project(":common").sourceSets.main.get().java.srcDirs)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
    
    // 包含 common 模块的资源文件
    from(project(":common").sourceSets.main.get().resources.srcDirs)
}
