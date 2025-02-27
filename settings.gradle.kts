plugins {
    id("org.zaproxy.common.settings") version "0.5.0"

    id("com.diffplug.spotless") version "6.25.0" apply false
}

rootProject.name = "gradle-plugin-add-on"

include("apigen")

rootProject.children.forEach { project -> setUpProject(settingsDir, project) }

fun setUpProject(
    parentDir: File,
    project: ProjectDescriptor,
) {
    project.projectDir = File(parentDir, project.name)
    project.buildFileName = "${project.name}.gradle.kts"

    if (!project.projectDir.isDirectory) {
        throw AssertionError("Project ${project.name} has no directory: ${project.projectDir}")
    }
    if (!project.buildFile.isFile) {
        throw AssertionError("Project ${project.name} has no build file: ${project.buildFile}")
    }
    project.children.forEach { setUpProject(it.parent!!.projectDir, it) }
}
