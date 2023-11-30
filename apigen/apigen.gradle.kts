plugins {
    `java-library`

    id("com.diffplug.spotless")
    id("org.zaproxy.common")
}

repositories {
    mavenCentral()
}

description = "The utility to help generate ZAP API client files for an add-on."

dependencies {
    compileOnly("org.zaproxy:zap:2.13.0")
}

tasks.jar.configure {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    manifest {
        attributes(mapOf("Class-Path" to "resources/"))
    }
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
