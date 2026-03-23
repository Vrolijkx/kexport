plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("kexport-annotation")
                description.set("Annotations for the kexport Gradle plugin")
                url.set("https://github.com/Vrolijkx/kexport")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("Vrolijkx")
                        name.set("Vrolijkx")
                    }
                }
                scm {
                    url.set("https://github.com/Vrolijkx/kexport")
                    connection.set("scm:git:git://github.com/Vrolijkx/kexport.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Vrolijkx/kexport.git")
                }
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY").orNull
    val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD").orNull
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}
