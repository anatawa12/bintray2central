package com.anatawa12.bintray2Central

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Polygon
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import javax.swing.*

const val TEXT_COLUMNS = 30

class MainPanel : JPanel() {
    private val sourceRepo: RepositoryPanel
    private val destRepo: RepositoryPanel
    private val pgpInfo: PGPInfoPanel
    private val log: JTextArea
    private val upload: JButton

    init {
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            sourceRepo = RepositoryPanel("Your bintray repository").also { add(it) }
            BigRightArrow().also { arrow ->
                arrow.preferredSize = Dimension(100, height)
            }.also { add(it) }
            destRepo = RepositoryPanel("Your maven central repository",
                listOf(
                    //CredentialsDisabledJButton("load from maven"),
                    JButton("load from gradle").apply {
                        addActionListener { loadCredentialsFromGradle() }
                    },
                )
            ).also { add(it) }
        }.also { add(it) }
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            pgpInfo = PGPInfoPanel(
                buttons = listOf(
                    //CredentialsDisabledJButton("load from maven"),
                    JButton("load from gradle").apply {
                        addActionListener { loadGPGInfoFromGradle() }
                    },
                )
            ).also { add(it) }
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(JLabel("log"))
                log = JTextArea().also { add(JScrollPane(it)) }
            })
        }.also { add(it) }

        upload = JButton("Upload!")
            .also { it.alignmentX = CENTER_ALIGNMENT }
            .also { add(it) }
    }

    init {
        destRepo.repository = centralUploadRepository
        destRepo.credentialsEnabled = true
        pgpInfo.pgpInfo = initialPgpInfo

        upload.addActionListener { upload() }

        instance = this
        logHandler = { string ->
            log.append("$string${System.lineSeparator()}")
            log.caretPosition = log.document.length
        }
    }

    var running = false

    fun upload() {
        if (running) {
            JOptionPane.showMessageDialog(this, "upload running")
            return
        }
        @Suppress("DeferredResultUnused")
        BintrayToCentralScope.async {
            try {
                running = true
                runUpload()
            } catch (throwable: Throwable) {
                log(throwable.getPrintStackTrace())
                JOptionPane.showMessageDialog(this@MainPanel, "some error was happened.\n" +
                        "see log for more details")
            } finally {
                running = false
            }
        }
    }

    private suspend fun runUpload() {
        fun URI.fillSchema(): URI = when {
            scheme != null -> this
            host != null -> copy(scheme = "http")
            else -> copy(scheme = "file")
        }
        fun URI.checkSchema(): URI? = when (scheme) {
            "file" -> this
            "http" -> this
            "https" -> this
            else -> {
                JOptionPane.showMessageDialog(this@MainPanel, "unsupported schema: $scheme")
                null
            }
        }

        val sourceRepo = sourceRepo.repository.checkOrAddLastSlash().buildURI().fillSchema().checkSchema() ?: return
        val destRepo = destRepo.repository.checkOrAddLastSlash().buildURI().fillSchema().checkSchema() ?: return
        val pgpInfo = pgpInfo.pgpInfo

        var includeSignAndHash = true

        if (destRepo.scheme == "file") {
            val result = JOptionPane.showConfirmDialog(this,
                "Your destination repository is local directory.\n" +
                        "Do you want to add pgp signature and hashes?",
                "destination repository is local",
                JOptionPane.YES_NO_OPTION)
            if (result == JOptionPane.NO_OPTION) {
                includeSignAndHash = false
            }
        }

        HttpClient().use { client ->
            log("initializing source repository loader")
            val tracer = when (sourceRepo.scheme) {
                "http", "https" -> BintrayTracer(client, sourceRepo)
                "file" -> LocalTracer(File(sourceRepo))
                else -> throw NotImplementedError("never here")
            }
            var fileFlow: FileFlow = tracer.trace()
                .filterIsInstance<Entry.File>()

            if (includeSignAndHash) {
                log("initializing pgp")
                val signer = createSigner(pgpInfo)
                log("initializing hashing algorithms")
                val hasher = createHasher()
                fileFlow = fileFlow
                    .then(signer)
                    .then(hasher)
            }

            log("initializing destination repository uploader")
            val createPutter = when (destRepo.scheme) {
                "http", "https" -> HttpPutter(client = client, baseUri = destRepo)
                "file" -> LocalPutter(File(destRepo))
                else -> throw NotImplementedError("never here")
            }
            fileFlow = fileFlow.onEach { com.anatawa12.bintray2Central.log("processing: ${it.relativePath}") }
            log("starting upload...")
            fileFlow.collect(createPutter)
        }
        log("uploading finished!")
    }

    private fun loadCredentialsFromGradle() {
        @Suppress("DeferredResultUnused")
        BintrayToCentralScope.async {
            try {
                loadCredentialsFromGradleInternal()
            } catch (throwable: Throwable) {
                log(throwable.getPrintStackTrace())
            }
        }
    }

    private suspend fun loadCredentialsFromGradleInternal() {
        val props = getGradleProperties() ?: return

        val credentials = props.getCredentials() ?: kotlin.run {
            JOptionPane.showMessageDialog(this, "no credentials ware found")
            log("no credentials in gradle.properties")
            return
        }
        val repository = destRepo.repository
        destRepo.repository = Repository(
            url = repository.url,
            user = credentials.user.takeUnless { it.isEmpty() } ?: repository.user,
            pass = credentials.pass.takeUnless { it.isEmpty() } ?: repository.pass,
        )
    }

    private fun loadGPGInfoFromGradle() {
        val props = getGradleProperties() ?: return

        val pgpInfoByGradle = props.getPGPInfo()
        val pgpInfo = pgpInfo.pgpInfo
        this.pgpInfo.pgpInfo = PGPInfo(
            secring = pgpInfoByGradle.secring.takeUnless { it.isEmpty() } ?: pgpInfo.secring,
            keyId = pgpInfoByGradle.keyId.takeUnless { it.isEmpty() } ?: pgpInfo.keyId,
            password = pgpInfoByGradle.password.takeUnless { it.isEmpty() } ?: pgpInfo.password,
        )
    }

    fun getGradleProperties(): GradleProperties? {
        try {
            return GradleProperties.read()
        } catch (e: FileNotFoundException) {
            JOptionPane.showMessageDialog(this, "gradle.properties file in " +
                    "gradle user directory not found")
            log("no gradle.properties: " + e.getPrintStackTrace())
            return null
        } catch (e: IOException) {
            JOptionPane.showMessageDialog(this, "gradle.properties file in " +
                    "gradle user directory is not valid")
            log("invalid gradle.properties: " + e.getPrintStackTrace())
            return null
        } catch (e: CancellationException) {
            return null
        }
    }

    companion object {
        val centralUploadRepository = Repository(url = "https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        val initialPgpInfo = PGPInfo(
            secring = java.io.File(System.getProperty("user.home")).resolve(".gnupg/secring.gpg").absolutePath,
            keyId = "",
            password = "",
        )

        lateinit var instance: MainPanel
            private set
    }
}

class DisabledJButton : JButton {
    constructor() : super()
    constructor(icon: Icon?) : super(icon)
    constructor(text: String?) : super(text)
    constructor(a: Action?) : super(a)
    constructor(text: String?, icon: Icon?) : super(text, icon)

    override fun isEnabled(): Boolean = false
    override fun setEnabled(b: Boolean) {
    }

    init {
        isEnabled = false
    }
}

class RepositoryPanel(
    repoName: String,
    buttons: List<JButton> = listOf(),
) : JPanel() {
    private fun <T: JComponent> T.addElem() = also {
        it.alignmentX = LEFT_ALIGNMENT
        this@RepositoryPanel.add(it)
        elements.add(it)
    }

    private val elements = mutableListOf<JComponent>()
    private val repoNameLabel = JPanel().apply { add(JLabel(repoName)) }.keepHeight().addElem()
    private val urlLabel = JLabel("URL:").keepHeight().addElem()
    private val urlField = JTextField(TEXT_COLUMNS).keepHeight().addElem()
    private val credentialsCheck = JCheckBox("use credentials").keepHeight().addElem()
    private val credentialsPanel = CredentialsPanel(buttons).keepHeight().addElem()

    init {
        alignmentY = TOP_ALIGNMENT
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        for (element in elements) {
            element.alignmentX = LEFT_ALIGNMENT
            element.keepHeight()
            add(element)
        }

        credentialsPanel.isEnabled = credentialsCheck.isSelected
        credentialsCheck.addActionListener { _ ->
            credentialsPanel.isEnabled = credentialsCheck.isSelected
        }
        keepHeight()
    }

    var repository: Repository
        get() = if (credentialsEnabled) {
            Repository(urlField.text, credentialsPanel.user, credentialsPanel.pass)
        } else {
            Repository(urlField.text)
        }
        set(value) {
            urlField.text = value.url
            if (value.user != "" && value.pass != "")
                credentialsEnabled = true
            if (credentialsEnabled) {
                credentialsPanel.user = value.user
                credentialsPanel.pass = value.pass
            }
        }

    var credentialsEnabled: Boolean
        get() = credentialsCheck.isSelected
        set(value) {
            credentialsCheck.isSelected = value
            credentialsPanel.isEnabled = value
        }
}

class CredentialsPanel(
    buttons: List<JButton> = listOf(),
): JPanel() {
    private fun <T: JComponent> T.addElem() = also {
        it.alignmentX = LEFT_ALIGNMENT
        this@CredentialsPanel.add(it)
        elements.add(it)
    }

    private val elements = mutableListOf<JComponent>()
    private val userLabel = JLabel("user:").keepHeight().addElem()
    private val userField = JTextField(TEXT_COLUMNS).keepHeight().addElem()
    private val passLabel = JLabel("password:").keepHeight().addElem()
    private val passField = JTextField(TEXT_COLUMNS).keepHeight().addElem()
    private val buttons = JPanel().apply {
        buttons.map { button ->
            add(button.also { elements.add(it) })
        }
    }.keepHeight().addElem()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        keepHeight()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        elements.forEach { it.isEnabled = enabled }
    }

    var user: String by userField::text
    var pass: String by passField::text
}

class PGPInfo(
    val secring: String,
    val keyId: String,
    val password: String,
) {
}

class PGPInfoPanel(
    repoName: String = "PGP(GnuPG) Information",
    buttons: List<JButton> = listOf(),
) : JPanel() {
    private fun <T: JComponent> T.addElem() = also {
        it.alignmentX = LEFT_ALIGNMENT
        this@PGPInfoPanel.add(it)
        elements.add(it)
    }

    private val elements = mutableListOf<JComponent>()
    private val repoNameLabel = JPanel().apply { add(JLabel(repoName)) }.keepHeight().addElem()
    private val secringLabel = JLabel("secring.gpg path:").keepHeight().addElem()
    private val secringField = JTextField(TEXT_COLUMNS).keepHeight().addElem()
    private val keyIdLabel = JLabel("keyId:").keepHeight().addElem()
    private val keyIdField = JTextField(TEXT_COLUMNS).keepHeight().addElem()
    private val passwordLabel = JLabel("key passphrase:").keepHeight().addElem()
    private val passwordField = JTextField(TEXT_COLUMNS).keepHeight().addElem()
    private val buttons = JPanel().apply {
        buttons.map { button ->
            add(button.also { elements.add(it) })
        }
    }.keepHeight().addElem()

    init {
        alignmentY = TOP_ALIGNMENT
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        for (element in elements) {
            element.alignmentX = LEFT_ALIGNMENT
            element.keepHeight()
            add(element)
        }
        keepHeight()
    }

    var pgpInfo: PGPInfo
        get() = PGPInfo(secringField.text, keyIdField.text, passwordField.text)
        set(value) {
            secringField.text = value.secring
            keyIdField.text = value.keyId
            passwordField.text = value.password
        }
}

class BigRightArrow : Component() {
    override fun paint(g: Graphics) {
        g.fillRect(0, height / 4,
            width / 2, height / 2)

        g.fillPolygon(Polygon().apply {
            addPoint(width / 2, 0)
            addPoint(width / 1, height / 2)
            addPoint(width / 2, height / 1)
        })
    }
}

fun <T: Component> T.keepHeight() = apply { maximumSize = preferredSize }
