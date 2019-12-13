import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.jfrog.bintray") version "1.8.4"
    id("maven-publish")
    kotlin("jvm") version "1.3.61"
    id("org.jetbrains.dokka") version "0.9.18"
}

group = "com.molikuner.sqldelight"
version = "1.2.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.squareup.sqldelight:sqlite-driver:$version")
    implementation("com.squareup.sqldelight:runtime-jvm:$version")
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

fun getProperty(project: String, environment: String): String? {
    return if (this.project.hasProperty(project)) {
        this.project.property(project) as? String?
    } else {
        System.getenv(environment)
    }
}

bintray {
    user = getProperty(project = "bintray.user", environment = "BINTRAY_USER")
    key = getProperty(project = "bintray.apiKey", environment = "BINTRAY_API_KEY")
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
            gpg(delegateClosureOf<BintrayExtension.GpgConfig> {
                sign = true
                passphrase = getProperty(project = "bintray.gpg.passphrase", environment = "BINTRAY_GPG_PASSPHRASE")
            })
        })
        publish = true
    })
}
