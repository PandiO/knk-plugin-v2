repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API for Bukkit/Spigot compatibility (compileOnly - API is provided at runtime)
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    
    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Paper API for tests (needed for Vector class in unit tests)
    testImplementation("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

tasks.test {
    useJUnitPlatform()
}
