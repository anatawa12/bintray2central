package com.anatawa12.bintray2Central

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder
import java.io.ByteArrayOutputStream


class Signer(
    private val privateKey: PGPPrivateKey,
    private val publicKey: PGPPublicKey,
    private val condition: (Entry.File) -> Boolean = { true },
): AddingFileTransformer {
    fun sign(file: ByteArray): ByteArray {
        val sigGen = PGPSignatureGenerator(BcPGPContentSignerBuilder(publicKey.algorithm, PGPUtil.SHA512))
        sigGen.init(PGPSignature.BINARY_DOCUMENT, privateKey)
        sigGen.update(file)
        val sign = sigGen.generate()
        val baos = ByteArrayOutputStream()
        val aos = ArmoredOutputStream(baos)
        aos.use { _ ->
            sign.encode(aos)
        }
        @Suppress("UsePropertyAccessSyntax")
        return baos.toByteArray()
    }

    override fun transformAdding(file: Entry.File): List<Entry.File> {
        if (!condition(file)) return emptyList()
        return listOf(Entry.File(
            relativePath = "${file.relativePath}.asc",
            content = sign(file.content)
        ))
    }
}
