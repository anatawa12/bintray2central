package com.anatawa12.bintray2Central

import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.hc.client5.http.async.methods.SimpleBody
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.io.entity.ByteArrayEntity
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.reactor.DefaultConnectingIOReactor
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * simple asynchronous http client
 */
class HttpClient : Closeable {
    private val client: CloseableHttpAsyncClient = HttpAsyncClients
        .custom()
        .setConnectionManager(PoolingAsyncClientConnectionManager())
        .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1)
        .build()

    init {
        client.start()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun get(uri: URI): ByteArray = runGet(uri).bodyBytes

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getString(uri: URI): String = runGet(uri).bodyText

    private suspend fun runGet(uri: URI): SimpleBody {
        val httpGet = SimpleHttpRequest("GET", uri)
        val result = callFeature<SimpleHttpResponse> { client.execute(httpGet, it) }
        if (result.code != 200)
            throw IllegalStateException("invalid response: ${result.code}: $result")
        return result.body
    }

    override fun close() {
        client.close()
    }

    suspend fun put(uri: URI, content: ByteArray) {
        val httpGet = SimpleHttpRequest("PUT", uri)
        httpGet.setBody(content, ContentType.APPLICATION_OCTET_STREAM)
        val result = callFeature<SimpleHttpResponse> { client.execute(httpGet, it) }
        if (result.code != 200 && result.code != 201)
            throw IllegalStateException("invalid response: ${result.code}: $result")
    }

    private suspend inline fun <T> callFeature(crossinline block: (FutureCallback<T>) -> Future<T>): T = suspendCancellableCoroutine { co ->
        val feature = block(object : FutureCallback<T> {
            override fun completed(result: T) {
                co.resume(result)
            }

            override fun failed(ex: Exception) {
                co.resumeWithException(ex)
            }

            override fun cancelled() = co.resumeWithException(CancellationException())
        })
        co.invokeOnCancellation { feature.cancel(true) }
    }
}
