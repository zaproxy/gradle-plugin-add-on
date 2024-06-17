package org.zaproxy.gradle.addon.internal.model

import java.io.File

data class GitHubRepo(val owner: String, val name: String, val dir: File? = null) {
    constructor(ownerAndName: String?, dir: File?) : this(
        split(ownerAndName, 0),
        split(ownerAndName, 1),
        dir,
    )

    override fun toString() = "$owner/$name"
}

fun split(
    value: String?,
    pos: Int,
): String {
    val values = value?.split('/')
    return if (values?.size == 2) values[pos] else ""
}
