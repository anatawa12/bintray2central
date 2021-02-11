package com.anatawa12.bintray2Central

import java.io.File
import java.io.IOException

class LocalTracer(
    private val base: File,
): AbstractTracer() {
    /**
     * relativePath requires ends with '/'
     */
    override suspend fun getDirectory(relativePath: String): Entry.Directory {
        val path = base.resolve(relativePath)
        try {
            val entries = path.listFiles()!!.map {
                val name = it.name
                if (it.isDirectory) {
                    DirectoryEntry.Directory(name)
                } else {
                    DirectoryEntry.File(name)
                }
            }.toList()
            return Entry.Directory(relativePath, entries)
        } catch (ex: IOException) {
            throw IOException("getting $path", ex)
        }
    }

    override suspend fun getFile(relativePath: String): Entry.File {
        val path = base.resolve(relativePath)
        try {
            return Entry.File(
                relativePath,
                path.readBytes(),
            )
        } catch (ex: IOException) {
            throw IllegalStateException("getting $path", ex)
        }
    }
}
