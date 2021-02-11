package com.anatawa12.bintray2Central

import java.io.IOException
import java.net.URI

class BintrayTracer(
    private val client: HttpClient,
    private val base: URI,
): AbstractTracer() {
    /**
     * relativePath requires ends with '/'
     */
    override suspend fun getDirectory(relativePath: String): Entry.Directory {
        val url = base.resolve(relativePath)
        try {
            val html = client.getString(url)
            val entries = hrefRegex.findAll(html).map {
                val name = it.groupValues[1].removePrefix(":")
                if (name.endsWith("/")) {
                    DirectoryEntry.Directory(name.removeSuffix("/"))
                } else {
                    DirectoryEntry.File(name)
                }
            }.toList()
            return Entry.Directory(relativePath, entries)
        } catch (ex: IOException) {
            throw IOException("getting $url", ex)
        }
    }

    override suspend fun getFile(relativePath: String): Entry.File {
        val url = base.resolve(relativePath)
        try {
            return Entry.File(
                relativePath,
                client.get(url),
            )
        } catch (ex: IOException) {
            throw IllegalStateException("getting $url", ex)
        }
    }

    companion object {
        val hrefRegex = """href="([^"]+)"""".toRegex()
    }
}
