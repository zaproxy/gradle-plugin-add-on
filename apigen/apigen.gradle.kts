plugins {
    `java-library`

    id("com.diffplug.spotless")
}

repositories {
    mavenCentral()
}

description = "The utility to help generate ZAP API client files for an add-on."

dependencies {
    compileOnly("org.zaproxy:zap:2.8.0")
}

tasks.jar.configure {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    manifest {
        attributes(mapOf("Class-Path" to "resources/"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-Xlint:all", "-Xlint:-path", "-Xlint:-options", "-Werror")
}

spotless {
    java {
        licenseHeaderFile("$rootDir/gradle/spotless/license.java")
        googleJavaFormat().aosp()
    }

    kotlinGradle {
        ktlint()
    }
}
