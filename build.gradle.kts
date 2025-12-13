allprojects {
    group = "com.knk"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    // Zorgt dat elk subproject de Java plugin heeft
    plugins.apply("java")

    // Zet JDK 21 toolchain voor alle subprojects
    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain {
            languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
        }
    }

    // Encoding consistent (optioneel, maar netjes)
    tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}
