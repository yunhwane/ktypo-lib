plugins {
    kotlin("jvm") version "2.2.21"
    `java-library`
    `maven-publish`
}

group = "com.yunhwan"
version = "0.0.1-SNAPSHOT"
description = "ktypo - Swagger + RestDocs auto-generation Kotlin DSL library"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3")
    api("io.swagger.core.v3:swagger-models:2.2.28")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "ktypo"
            version = project.version.toString()

            pom {
                name.set("ktypo")
                description.set("Kotlin DSL library for auto-generating OpenAPI 3.1 specs and RestDocs Asciidoc snippets")
                url.set("https://github.com/yunhwane/ktypo-lib")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("yunhwane")
                        name.set("yunhwan")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/yunhwane/ktypo-lib.git")
                    developerConnection.set("scm:git:ssh://github.com/yunhwane/ktypo-lib.git")
                    url.set("https://github.com/yunhwane/ktypo-lib")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/yunhwane/ktypo-lib")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
