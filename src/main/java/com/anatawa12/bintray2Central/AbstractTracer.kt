package com.anatawa12.bintray2Central

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.net.URI

abstract class AbstractTracer() {
    /**
     * relativePath requires ends with '/'
     */
    fun trace(): Flow<Entry> = flow {
        traceDir("")
    }

    /**
     * relativePath requires ends with '/'
     */
    private suspend fun FlowCollector<Entry>.traceDir(relativePath: String): Unit = coroutineScope {
        val dirEntry = getDirectory(relativePath)
        emit(dirEntry)
        val deferreds = mutableListOf<Deferred<Entry.File>>()
        for (entry in dirEntry.entries) {
            if (entry.isDirectory) {
                traceDir("$relativePath${entry.name}/")
            } else {
                deferreds += async { getFile("$relativePath${entry.name}") }
            }
            delay(10)
        }
        for (deferred in deferreds) {
            emit(deferred.await())
        }
    }

    /**
     * relativePath requires ends with '/'
     */
    protected abstract suspend fun getDirectory(relativePath: String): Entry.Directory

    protected abstract suspend fun getFile(relativePath: String): Entry.File

    companion object {
        val hrefRegex = """href="([^"]+)"""".toRegex()
    }
}
