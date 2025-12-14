val devServerDirPath = (findProperty("devServerDirectory") as String?) ?: "../../DEV_SERVER_1.21.10"

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":knk-core"))
    implementation(project(":knk-api-client"))

    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    
    testImplementation("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val devPluginsDir = rootProject.layout.projectDirectory
    .dir(devServerDirPath)
    .dir("plugins")

tasks {
    shadowJar {
        archiveClassifier.set("")
    }

    register<Copy>("deployToDevServer") {
        dependsOn(shadowJar)
        mustRunAfter(jar)

        from(shadowJar.get().archiveFile)
        into(devPluginsDir)

        doFirst {
            println("Deploying plugin jar to: ${devPluginsDir.asFile.absolutePath}")
        }
    }

    // “Dev” convenience task: build (shadowJar) + deploy
    register("dev") {
        dependsOn("shadowJar")
        dependsOn("deployToDevServer")
    }

    // Optioneel: laat een normale build ook deployen
    build {
        dependsOn("deployToDevServer")
    }
}
