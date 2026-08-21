plugins {
    id("dev.architectury.loom")
    jacoco
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
    })

    compileOnly("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    compileOnly("net.fabricmc:sponge-mixin:0.15.4+mixin.0.8.7")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testCompileOnly("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
