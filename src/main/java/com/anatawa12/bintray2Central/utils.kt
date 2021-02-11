package com.anatawa12.bintray2Central

import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI

fun Throwable.getPrintStackTrace(): String = StringWriter()
    .also { sw -> PrintWriter(sw).use { printStackTrace(it) } }.toString()

var logHandler: ((String) -> Unit)? = null

fun log(string: String) {
    logHandler?.invoke(string)
    println(string)
}

fun URI.copy(
    scheme: String? = this.scheme,
    userInfo: String? = this.userInfo,
    host: String? = this.host,
    port: Int = this.port,
    path: String? = this.path,
    query: String? = this.query,
    fragment: String? = this.fragment,
) = URI(
    scheme,
    userInfo,
    host,
    port,
    path,
    query,
    fragment,
)
