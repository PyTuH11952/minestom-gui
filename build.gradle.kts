plugins {
    id("java")
    id("maven-publish")
}

group = "com.minebreak"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.01.08-1.21.11")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/PyTuH11952/minestom-gui")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
