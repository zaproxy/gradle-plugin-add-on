plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.0"

    id("com.diffplug.gradle.spotless") version "3.18.0"
}

repositories {
    jcenter()
}

group = "org.zaproxy.gradle"
version = "0.2.0-SNAPSHOT"

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.9")
    implementation("com.overzealous:remark:1.1.0")
    val flexmarkVersion = "0.42.8"
    implementation("com.vladsch.flexmark:flexmark-java:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-strikethrough:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-gfm-tasklist:$flexmarkVersion")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:$flexmarkVersion")
    implementation("commons-codec:commons-codec:1.12")
    implementation("io.github.classgraph:classgraph:4.8.36")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("org.zaproxy:zap-clientapi:1.6.0")
    implementation("org.kohsuke:github-api:1.95")
    // Include annotations used by the above library to avoid compiler warnings.
    compileOnly("com.google.code.findbugs:findbugs-annotations:3.0.1")
    compileOnly("com.infradna.tool:bridge-method-annotation:1.18")
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

tasks.validateTaskProperties {
    enableStricterValidation = true
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
        googleJavaFormat().aosp()
    }

    kotlinGradle {
        ktlint()
    }
}