import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("maven-publish")
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.dokka") version "1.6.21"
    signing
}

group = "com.molikuner.sqldelight"
version = "1.4.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.sqldelight:sqlite-driver:$version")
    implementation("com.squareup.sqldelight:runtime-jvm:$version")
}

kotlin {
    explicitApi()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(dokkaJar)
            artifact(sourcesJar)
        }
    }
    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}

fun getProperty(projectKey: String, environmentKey: String): String? {
    return if (project.hasProperty(projectKey)) {
        project.property(projectKey) as? String?
    } else {
        System.getenv(environmentKey)
    }
}

signing {
    val signingKey = getProperty(projectKey = "gpg.key", environmentKey = "GPG_KEY")
    if (signingKey == null) logger.warn("The GPG key for signing was not found. Either provide it as env variable 'GPG_KEY' or as project property 'gpg.key'. Otherwise the signing will fail!")
    val signingPassword = getProperty(projectKey = "gpg.passphrase", environmentKey = "GPG_PASSPHRASE")
    if (signingPassword == null) logger.warn("The passphrase for the signing key was not found. Either provide it as env variable 'GPG_PASSPHRASE' or as project property 'gpg.passphrase'. Otherwise the signing might fail!")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["default"])
}
