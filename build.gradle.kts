plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.13.0"

    id("com.diffplug.spotless") version "5.11.0"
}

repositories {
    mavenCentral()
}

group = "org.zaproxy.gradle"
version = "0.5.0-SNAPSHOT"

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.9")
    val flexmarkVersion = "0.42.8"
    implementation("com.vladsch.flexmark:flexmark-java:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-tasklist:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:$flexmarkVersion")
    implementation("io.github.classgraph:classgraph:4.8.36")
    implementation("org.zaproxy:zap-clientapi:1.9.0")
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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-Xlint:all", "-Xlint:-path", "-Xlint:-options", "-Werror")
}

val pluginName = "add-on"
gradlePlugin {
    (plugins) {
        create(pluginName) {
            id = "org.zaproxy.add-on"
            implementationClass = "org.zaproxy.gradle.addon.AddOnPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/zaproxy/gradle-plugin-add-on"
    vcsUrl = "https://github.com/zaproxy/gradle-plugin-add-on.git"
    description = "A Gradle plugin to (help) build ZAP add-ons."
    tags = listOf("zap", "zaproxy")

    (plugins) {
        pluginName {
            displayName = "Plugin to build ZAP add-ons"
        }
    }
}

spotless {
    java {
        licenseHeaderFile("gradle/spotless/license.java")
        googleJavaFormat("1.7").aosp()
    }

    kotlinGradle {
        ktlint()
    }
}
