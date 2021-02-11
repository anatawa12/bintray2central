package com.anatawa12.bintray2Central

import java.io.File

class LocalPutter(
    val base: File,
) : FileFlowEdge {
    override suspend fun accept(file: Entry.File) {
        val filePath = base.resolve(file.relativePath)
        filePath.parentFile!!.mkdirs()
        filePath.writeBytes(file.content)
    }
}
