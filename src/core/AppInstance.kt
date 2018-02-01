package core

import components.RecordingsListPane
import java.awt.event.WindowEvent
import java.awt.event.WindowListener

import java.util.*
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JPanel

const val FRAME_TITLE = "NoteWize"

object AppInstance : JFrame(FRAME_TITLE), WindowListener {

    private val paneStack: Stack<ApplicationPane> = Stack()

    fun start() {

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

            System.exit(0)

        } else {

            contentPane = paneStack.peek()
            pack()
            setLocationRelativeTo(null)

            paneStack.peek().onResume()

            repaint()

        }

    }

    private fun popAll() {

        paneStack.peek().onPause()
        paneStack.pop().onDestroy()

        while (paneStack.isNotEmpty()) {
            paneStack.pop().onDestroy()
        }

        System.exit(0)

    }

    override fun windowActivated(e: WindowEvent?) {}
    override fun windowDeactivated(e: WindowEvent?) {}
    override fun windowOpened(e: WindowEvent?) {}
    override fun windowClosing(e: WindowEvent?) {
        dispose()
    }

    override fun windowClosed(e: WindowEvent?) {
        popAll()
    }

    override fun windowIconified(e: WindowEvent?) {}

    override fun windowDeiconified(e: WindowEvent?) {}

    abstract class ApplicationPane : JPanel() {
        abstract fun onCreate()
        abstract fun onPause()
        abstract fun onResume()

        abstract fun onDestroy()

    }

}