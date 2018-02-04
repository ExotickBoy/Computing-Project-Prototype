package core

import components.RecordingsListPane
import java.awt.event.WindowEvent
import java.awt.event.WindowListener

import java.util.*
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

const val FRAME_TITLE: String = "NoteWize"

object AppInstance : JFrame(FRAME_TITLE), WindowListener {

    private val paneStack: Stack<ApplicationPane> = Stack()

    fun start() {

        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(this)

        iconImage = ImageIcon("res/icon.png").image
        push(RecordingsListPane())
        isVisible = true

    }

    fun push(pane: ApplicationPane) {

        if (paneStack.isNotEmpty())
            paneStack.peek().onPause()

        paneStack.push(pane)

        pane.onCreate()
        contentPane = pane
        pack()
        setLocationRelativeTo(null)

        pane.onResume()

        repaint()

    }

    fun pop() {

        paneStack.peek().onPause()
        paneStack.pop().onDestroy()

        if (paneStack.isEmpty()) {

            dispose()
            System.exit(0)

        } else {

            contentPane = paneStack.peek()
            pack()
            setLocationRelativeTo(null)

            paneStack.peek().onResume()

            repaint()

        }

    }

    internal fun popAll() {

        paneStack.peek().onPause()
        paneStack.pop().onDestroy()

        while (paneStack.isNotEmpty()) {
            paneStack.pop().onDestroy()
        }

        dispose()
        System.exit(0)

    }

    override fun windowActivated(e: WindowEvent?) {}
    override fun windowDeactivated(e: WindowEvent?) {}
    override fun windowOpened(e: WindowEvent?) {}
    override fun windowClosing(e: WindowEvent?) {
        paneStack.peek().onClose()
    }

    override fun windowClosed(e: WindowEvent) {}
    override fun windowIconified(e: WindowEvent?) {}
    override fun windowDeiconified(e: WindowEvent?) {}

    abstract class ApplicationPane : JPanel() {
        abstract fun onCreate()
        abstract fun onPause()
        abstract fun onResume()
        abstract fun onDestroy()
        abstract fun onClose()
    }

}