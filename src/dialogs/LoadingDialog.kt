package dialogs

import java.awt.BorderLayout
import java.awt.Window
import javax.swing.*
import kotlin.concurrent.thread

public class LoadingDialog(owner: Window, labelText: String, title: String, action: () -> Unit) : JDialog(owner, title, ModalityType.APPLICATION_MODAL) {

    init {

        val content = JPanel(BorderLayout())
        content.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val label = JLabel(labelText)
        label.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

        val progressBar = JProgressBar()
        progressBar.isIndeterminate = true

        content.add(label, BorderLayout.NORTH)
        content.add(progressBar, BorderLayout.CENTER)

        contentPane = content
        pack()
        setLocationRelativeTo(owner)
        defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

        thread(name = "Loading Worker") {
            action.invoke()
            dispose()
        }
        isVisible = true

    }

}