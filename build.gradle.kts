plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = property("group") as String
version = property("version") as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveBaseName.set("AIWorkbench")
    }

    shadowJar {
        archiveBaseName.set("AIWorkbench")
        archiveClassifier.set("")
        relocate("com.google.gson", "com.aicraft.aiworkbench.libs.gson")
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
