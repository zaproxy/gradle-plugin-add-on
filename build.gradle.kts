plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"

    id("com.diffplug.spotless")
    id("org.zaproxy.common")
}

group = "org.zaproxy.gradle"
version = "0.11.0-SNAPSHOT"

dependencies {
    implementation("commons-codec:commons-codec:1.15")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.9")
    val flexmarkVersion = "0.42.8"
    implementation("com.vladsch.flexmark:flexmark-java:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-tasklist:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:$flexmarkVersion")
    implementation("io.github.classgraph:classgraph:4.8.36")
    val jgitVersion = "5.6.0.201912101111-r"
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit.archive:$jgitVersion")
    implementation("org.zaproxy:zap-clientapi:1.14.0")
    implementation("org.kohsuke:github-api:1.95")
    // Include annotations used by the above library to avoid compiler warnings.
    compileOnly("com.google.code.findbugs:findbugs-annotations:3.0.1")
    compileOnly("com.infradna.tool:bridge-method-annotation:1.18") {
        exclude(group = "org.jenkins-ci")
    }
    implementation("com.github.zafarkhaja:java-semver:0.9.0")
}

tasks.jar {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    into("org/zaproxy/gradle/addon/apigen/") {
        from(provider({ project(":apigen").tasks.named<Jar>("jar").flatMap { it.archiveFile } }))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
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
