plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.0"

    id("com.diffplug.gradle.spotless") version "3.16.0"
}

repositories {
    mavenCentral()
    jcenter()
}

group = "org.zaproxy.gradle"
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
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

    mavenCoordinates {
        groupId = "org.zaproxy.gradle"
        artifactId = "gradle-plugin-addon"
    }
}

spotless {
    java {
        licenseHeaderFile("gradle/spotless/license.java")
        googleJavaFormat().aosp()
    }
}