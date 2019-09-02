import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.jfrog.bintray") version "1.8.4"
    id("maven-publish")
    kotlin("jvm") version "1.3.50"
    id("org.jetbrains.dokka") version "0.9.18"
}

group = "com.molikuner.sqldelight"
version = "1.1.4"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.sqldelight:sqlite-driver:1.1.4")
    implementation("com.squareup.sqldelight:runtime-jvm:1.1.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(tasks.dokka)
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
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

bintray {
    user = if (project.hasProperty("bintray.user")) project.property("bintray.user") as String else System.getenv("BINTRAY_USER")
    key = if (project.hasProperty("bintray.apiKey")) project.property("bintray.apiKey") as String else System.getenv("BINTRAY_API_KEY")
    setPublications("default")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven-extensions"
        name = "sqldelight-simple-jvm-driver"
        userOrg = "molikuner"
        websiteUrl = "https://github.com/molikuner/sqldelight-simple-jvm-driver"
        githubRepo = "molikuner/sqldelight-simple-jvm-driver"
        vcsUrl = "https://github.com/molikuner/sqldelight-simple-jvm-driver.git"
        description = "Simple wrapper for default JVM driver of SQLDelight"
        setLabels("kotlin", "SQLDelight", "JVM", "driver", "SQL")
        setLicenses("Apache-2.0")
        desc = description
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            val rootVersion = rootProject.version as String
            name = rootVersion
            vcsTag = rootVersion
        })
    })
}
