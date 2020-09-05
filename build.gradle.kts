import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.jfrog.bintray") version "1.8.5"
    id("maven-publish")
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.dokka") version "1.4.0-rc"
}

group = "com.molikuner.sqldelight"
version = "1.4.3"

repositories {
    mavenCentral()
    jcenter()
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
    outputDirectory = "$buildDir/javadoc"
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

bintray {
    user = getProperty(projectKey = "bintray.user", environmentKey = "BINTRAY_USER")
    key = getProperty(projectKey = "bintray.apiKey", environmentKey = "BINTRAY_API_KEY")
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
                passphrase = getProperty(projectKey = "bintray.gpg.passphrase", environmentKey = "BINTRAY_GPG_PASSPHRASE")
            })
        })
        publish = true
    })
}
