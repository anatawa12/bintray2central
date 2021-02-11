package com.anatawa12.bintray2Central

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

object BintrayToCentralScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            .asCoroutineDispatcher()

}
