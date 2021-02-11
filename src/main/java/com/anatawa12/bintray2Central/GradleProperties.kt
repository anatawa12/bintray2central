package com.anatawa12.bintray2Central

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.IOException
import java.util.*
import javax.swing.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class GradleProperties(val props: Properties) {
    @Suppress("UNCHECKED_CAST")
    private val propsAsMap = props as Map<String, String>

    /**
     * return does not include url
     */
    suspend fun getCredentials(): Repository? {
        val userCandidates = propsAsMap.filter { it.key.contains("user", true) }
        val passCandidates = propsAsMap.filter { it.key.contains("pass", true) }
        if (userCandidates.isEmpty() && passCandidates.isEmpty()) return null
        val dialog = CredentialsSelectingDialog(userCandidates, passCandidates)
        return suspendCoroutine<Repository?> {
            dialog.onFinish = { user, pass -> it.resume(Repository("", user, pass)) }
            dialog.onCancel = { it.resumeWithException(CancellationException()) }
            dialog.isVisible = true
        }
    }

    fun getPGPInfo(): PGPInfo {
        return PGPInfo(
            secring = props.getProperty("signing.secretKeyRingFile") ?: "",
            keyId = props.getProperty("signing.keyId") ?: "",
            password = props.getProperty("signing.password") ?: "",
        )
    }

    companion object {
        private val gradleHome = kotlin.run {
            val gradleUserHome = System.getenv("GRADLE_USER_HOME")
            if (gradleUserHome != null) java.io.File(gradleUserHome)
            else java.io.File(System.getProperty("user.home")).resolve(".gradle")
        }
        val gradleUserPropertiesFile = gradleHome.resolve("gradle.properties")

        @Throws(IOException::class)
        fun read(): GradleProperties {
            val props = Properties()
            props.load(gradleUserPropertiesFile.inputStream())
            return GradleProperties(props)
        }

        class CredentialsSelectingDialog(
            val userCandidates: Map<String, String>,
            val passCandidates: Map<String, String>,
        ) : JDialog() {
            var onFinish: ((user: String, pass: String) -> Unit)? = null
            var onCancel: (() -> Unit)? = null

            init {
                title = "Credential select"

                layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)

                add(JLabel("Please select credentials").apply { alignmentX = CENTER_ALIGNMENT })
                add(JLabel("read from gradle.properties").apply { alignmentX = CENTER_ALIGNMENT })
                add(JLabel(" ").apply { alignmentX = CENTER_ALIGNMENT })
                add(JLabel("username").apply { alignmentX = CENTER_ALIGNMENT })
                val user = JComboBox<String>()
                userCandidates.entries.forEach { user.addItem(it.key) }
                add(user)

                add(JLabel("password").apply { alignmentX = CENTER_ALIGNMENT })
                val pass = JComboBox<String>()
                passCandidates.entries.forEach { pass.addItem(it.key) }
                add(pass)

                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)

                    add(JButton("cancel").apply {
                        //alignmentX = CENTER_ALIGNMENT

                        addActionListener {
                            dispose()
                            onCancel?.invoke()
                        }
                    })

                    add(JButton("set!").apply {
                        //alignmentX = CENTER_ALIGNMENT

                        addActionListener {
                            dispose()
                            onFinish?.invoke(
                                userCandidates[user.selectedItem as String] ?: "",
                                passCandidates[pass.selectedItem as String] ?: "",
                            )
                        }
                    })
                })

                isResizable = false
                pack()
                setLocationRelativeTo(MainPanel.instance)

                defaultCloseOperation = DO_NOTHING_ON_CLOSE
            }
        }
    }
}
