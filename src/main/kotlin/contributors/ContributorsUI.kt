package contributors

import kotlinx.coroutines.Job
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

private val INSETS = Insets(3, 10, 3, 10)
private val COLUMNS = arrayOf("ID", "Title")

@Suppress("CONFLICTING_INHERITED_JVM_DECLARATIONS")
class ContributorsUI : JFrame("Vimeo videos"), Contributors {
    override var counter: Int = 0
    private val username = JTextField(20)
    private val password = JPasswordField(20)
    private val org = JTextField(20)
    private val start = JTextField(20)
    private val end = JTextField(20)
    private val variant = JComboBox(Variant.values())
    private val load = JButton("Run")
    private val cancel = JButton("Cancel").apply { isEnabled = false }
    private val export = JButton("Export").apply { isEnabled = false }

    private val resultsModel = DefaultTableModel(COLUMNS, 0)
    private val results = JTable(resultsModel)
    private val resultsScroll = JScrollPane(results).apply {
        preferredSize = Dimension(200, 200)
    }

    private val loadingIcon = ImageIcon(javaClass.classLoader.getResource("ajax-loader.gif"))
    private val loadingStatus = JLabel("Start new loading", loadingIcon, SwingConstants.CENTER)

    override val job = Job()

    init {
        // Create UI

        for (info in UIManager.getInstalledLookAndFeels()) {
            if ("Metal" == info.name) {
                UIManager.setLookAndFeel(info.className)
                break
            }
        }
        export.addActionListener { writeVideosToFile() }
        rootPane.contentPane = JPanel(GridBagLayout()).apply {
            addLabeled("Start", start)
            addLabeled("End", end)
            addLabeled("Variant", variant)
            addWideSeparator()
            addWide(JPanel().apply {
                add(load)
                add(cancel)
                add(export)
            })
            addWide(resultsScroll) {
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
            addWide(loadingStatus)

        }
        // Initialize actions
        init()
    }

    override fun getSelectedVariant(): Variant = variant.getItemAt(variant.selectedIndex)

    override fun updateContributors(users: List<User>) {
        if (users.isNotEmpty()) {
            log.info("Updating result with ${users.size} rows")
        }
        else {
//            log.info("Clearing result")
        }
        resultsModel.setDataVector(users.map {
            arrayOf(it.login, it.contributions)
        }.toTypedArray(), COLUMNS)
    }
    override fun updateVideos(videos: List<Video>) {
        val toTypedArray = videos.map { arrayOf(it.id, it.title) }.toTypedArray()
        if (toTypedArray.isNotEmpty()) {
//            log.info("Updating result with ${toTypedArray.size} rows")
        }
        else {
//            log.info("Clearing result")
        }
        resultsModel.setDataVector(toTypedArray, COLUMNS)
    }

    override fun setLoadingStatus(text: String, iconRunning: Boolean) {
        loadingStatus.text = text
        loadingStatus.icon = if (iconRunning) loadingIcon else null
    }

    override fun addCancelListener(listener: ActionListener) {
        cancel.addActionListener(listener)
    }

    override fun removeCancelListener(listener: ActionListener) {
        cancel.removeActionListener(listener)
    }

    override fun addLoadListener(listener: () -> Unit) {
        load.addActionListener { listener() }
    }

    override fun addOnWindowClosingListener(listener: () -> Unit) {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                listener()
            }
        })
    }

    override fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean) {
        load.isEnabled = newLoadingEnabled
        cancel.isEnabled = cancellationEnabled
        export.isEnabled = cancellationEnabled
    }

    override fun setParams(params: Params) {
        username.text = params.username
        password.text = params.password
        org.text = params.org
        variant.selectedIndex = params.variant.ordinal
    }

    override fun getParams(): Params {
        return Params(username.text, password.password.joinToString(""), org.text, getSelectedVariant())
    }

    override fun getStartAndEnd(): Pair<Int, Int> {
        return Pair(start.text.toInt(), end.text.toInt())
    }
}

fun String.matchesVideoConstraint(): Boolean {
    return contains("greenius", ignoreCase = true)
}

fun JPanel.addLabeled(label: String, component: JComponent) {
    add(JLabel(label), GridBagConstraints().apply {
        gridx = 0
        insets = INSETS
    })
    add(component, GridBagConstraints().apply {
        gridx = 1
        insets = INSETS
        anchor = GridBagConstraints.WEST
        fill = GridBagConstraints.HORIZONTAL
        weightx = 1.0
    })
}

fun JPanel.addWide(component: JComponent, constraints: GridBagConstraints.() -> Unit = {}) {
    add(component, GridBagConstraints().apply {
        gridx = 0
        gridwidth = 2
        insets = INSETS
        constraints()
    })
}

fun JPanel.addWideSeparator() {
    addWide(JSeparator()) {
        fill = GridBagConstraints.HORIZONTAL
    }
}

fun setDefaultFontSize(size: Float) {
    for (key in UIManager.getLookAndFeelDefaults().keys.toTypedArray()) {
        if (key.toString().toLowerCase().contains("font")) {
            val font = UIManager.getDefaults().getFont(key) ?: continue
            val newFont = font.deriveFont(size)
            UIManager.put(key, newFont)
        }
    }
}