package com.anatawa12.bintray2Central

import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest
import java.security.Provider

class Hasher(
    val configurations: List<Configuration>,
    private val condition: (Entry.File) -> Boolean = { true },
): AddingFileTransformer {
    private fun hash(file: ByteArray, config: Configuration): ByteArray {
        val algorithm = config.algorithm
        val digest = algorithm.digest(file)
        return if (config.inHex) Hex.encode(digest) else digest
    }

    override fun transformAdding(file: Entry.File): List<Entry.File> {
        if (!condition(file)) return emptyList()
        return configurations.map { config ->
            Entry.File(
                relativePath = "${file.relativePath}.${config.extension}",
                content = hash(file.content, config)
            )
        }
    }

    data class Configuration(
        val extension: String,
        val algorithmProvider: Provider,
        val algorithmName: String,
        val inHex: Boolean,
    ) {
        internal val algorithm get() = MessageDigest.getInstance(algorithmName, algorithmProvider)
    }

    class Builder() {
        val configurations = mutableListOf<Configuration>()

        fun addHex(extension: String, algorithm: String) = add(extension, algorithm, true)

        fun add(extension: String, algorithm: String, inHex: Boolean = false) {
            configurations += Configuration(
                extension = extension,
                algorithmProvider = MessageDigest.getInstance(algorithm).provider,
                algorithmName = algorithm,
                inHex = inHex,
            )
        }

        fun build() = Hasher(configurations)
    }

    companion object {
        @Suppress("FunctionName")
        inline fun Builder(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}
