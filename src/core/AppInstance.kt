package core

import components.RecordingsListPane
import core.AppInstance.addWindowStateListener
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel

const val FRAME_TITLE = "NoteWize"

object AppInstance : JFrame(FRAME_TITLE) {

    private val paneStack: Stack<ApplicationPane> = Stack()

    fun start() {

        addWindowStateListener { println(it) }

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        push(RecordingsListPane())
        isVisible = true

    }

    fun push(pane: ApplicationPane) {

        if (!paneStack.isEmpty()) {

            paneStack.peek().onPause()

        }

        pane.onCreate()

        contentPane = pane
        pack()
        setLocationRelativeTo(null)

        pane.onResume()

    }

    fun pop() {

    }

    fun peek() {

    }

    abstract class ApplicationPane : JPanel() {

        abstract fun onCreate()
        abstract fun onPause()
        abstract fun onResume()
        abstract fun onDestroy()

    }

}