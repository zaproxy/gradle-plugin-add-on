plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"

    id("com.diffplug.spotless")
    id("org.zaproxy.common")
}

group = "org.zaproxy.gradle"
version = "0.13.0-SNAPSHOT"

dependencies {
    implementation("commons-codec:commons-codec:1.17.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.2")
    val flexmarkVersion = "0.64.8"
    implementation("com.vladsch.flexmark:flexmark-java:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-tasklist:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:$flexmarkVersion")
    implementation("io.github.classgraph:classgraph:4.8.179")
    val jgitVersion = "7.1.0.202411261347-r"
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit.archive:$jgitVersion")
    implementation("org.zaproxy:zap-clientapi:1.14.0")
    implementation("org.kohsuke:github-api:1.326")
    // Include annotations used by the above library to avoid compiler warnings.
    compileOnly("com.google.code.findbugs:findbugs-annotations:3.0.1")
    compileOnly("com.infradna.tool:bridge-method-annotation:1.18") {
        exclude(group = "org.jenkins-ci")
    }
    implementation("com.github.zafarkhaja:java-semver:0.10.2")
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    into("org/zaproxy/gradle/addon/apigen/") {
        from(provider({ project(":apigen").tasks.named<Jar>("jar").flatMap { it.archiveFile } }))
    }
}

java {
    val javaVersion = JavaVersion.VERSION_17
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    website.set("https://github.com/zaproxy/gradle-plugin-add-on")
    vcsUrl.set("https://github.com/zaproxy/gradle-plugin-add-on.git")
    plugins {
        create("add-on") {
            id = "org.zaproxy.add-on"
            implementationClass = "org.zaproxy.gradle.addon.AddOnPlugin"
            displayName = "Plugin to build ZAP add-ons"
            description = "A Gradle plugin to (help) build ZAP add-ons."
            tags.set(listOf("zap", "zaproxy"))
        }
    }
}

spotless {
    kotlin {
        ktlint()
    }

    kotlinGradle {
        ktlint()
    }
}
