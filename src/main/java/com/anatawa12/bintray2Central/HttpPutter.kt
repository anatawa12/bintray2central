package com.anatawa12.bintray2Central

import java.net.URI

class HttpPutter(
    val client: HttpClient,
    val baseUri: URI,
) : FileFlowEdge {
    override suspend fun accept(file: Entry.File) {
        client.put(
            baseUri.resolve(file.relativePath),
            file.content
        )
    }
}
