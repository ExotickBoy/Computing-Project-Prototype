package core

import components.RecordingsListPane
import core.AppInstance.addWindowStateListener
import java.util.*
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JPanel

const val FRAME_TITLE = "NoteWize"

object AppInstance : JFrame(FRAME_TITLE) {

    private val paneStack: Stack<ApplicationPane> = Stack()

    fun start() {

        addWindowStateListener { println(it) }

        iconImage = ImageIcon("D:\\computingProject\\icon2.png").image
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
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

        }

    }

    abstract class ApplicationPane : JPanel() {

        abstract fun onCreate()
        abstract fun onPause()
        abstract fun onResume()
        abstract fun onDestroy()

    }

}