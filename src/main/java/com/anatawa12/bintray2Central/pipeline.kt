package com.anatawa12.bintray2Central

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

typealias FileFlow = Flow<Entry.File>

interface AddingFileTransformer {
    fun transformAdding(file: Entry.File): List<Entry.File>
}

interface FileFlowEdge {
    suspend fun accept(file: Entry.File)
}

fun FileFlow.then(adder: AddingFileTransformer): FileFlow = transform { file ->
    emit(file)
    for (newFile in adder.transformAdding(file)) {
        emit(newFile)
    }
}

suspend fun FileFlow.collect(edge: FileFlowEdge) = coroutineScope {
    map { async { edge.accept(it) } }.collect { it.await() }
}
