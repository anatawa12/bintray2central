package com.anatawa12.bintray2Central

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRingCollection
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider
import java.io.FileInputStream
import java.net.URI
import java.security.Security
import javax.swing.JFrame

fun main() {
    Security.addProvider(BouncyCastleProvider())
    System.setProperty(
        kotlinx.coroutines.DEBUG_PROPERTY_NAME,
        kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
    )

    val panel = JFrame().apply {
        title = "bintray2central"
        MainPanel().also { add(it) }
        isResizable = false
        pack()
        setLocationRelativeTo(null)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }
    panel.isVisible = true
}

class Repository(
    var url: String,
    var user: String = "",
    var pass: String = "",
) {
    fun buildURI(): URI {
        val uri = URI(url)
        if (user == "" && pass == "") return uri
        return uri.copy(userInfo = if (pass == "") user else "$user:$pass")
    }

    fun checkOrAddLastSlash(): Repository = Repository(
        url.takeIf { it.endsWith('/') } ?: "$url/", user, pass
    )
}

fun createSigner(pgpInfo: PGPInfo): Signer {
    val keyId = KeyId(pgpInfo.keyId)
    val secring = FileInputStream(pgpInfo.secring)
        .let { PGPUtil.getDecoderStream(it) }
        .let { it.use { _ -> BcPGPSecretKeyRingCollection(it) } }

    val secretKey = secring.keyRings
        .asSequence()
        .flatMap { it.secretKeys.asSequence() }
        .first { KeyId(it.keyID) == keyId }

    val privateKey = secretKey.extractPrivateKey(BcPBESecretKeyDecryptorBuilder(BcPGPDigestCalculatorProvider())
        .build(pgpInfo.password.toCharArray()))
    val publicKey = secretKey.publicKey

    return Signer(
        privateKey = privateKey,
        publicKey = publicKey,
        condition = { file -> !file.relativePath.endsWith(".xml") },
    )
}

fun createHasher(): Hasher = Hasher.Builder {
    addHex("md5", "MD5")
    addHex("sha1", "SHA-1")
    addHex("sha256", "SHA-256")
    addHex("sha512", "SHA-512")
}

@OptIn(ExperimentalUnsignedTypes::class)
inline class KeyId constructor(private val uInt: UInt) {
    constructor(inString: String): this(
        kotlin.run {
            val normalized = inString.toUpperCase().removePrefix("0X")
            require(normalized.length == 8) { "keyId must be 8 chars excluding 0x" }
            normalized.toUInt(16)
        }
    )

    constructor(inLong: Long): this(inLong.toUInt())

    override fun toString(): String = uInt.toString(16)
}
