package components

import core.AppInstance
import core.Session
import dialogs.LoadingDialog
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.geom.Line2D
import javax.swing.*
import kotlin.math.max
import kotlin.math.min


class RecordingEditPane(val session: Session) : AppInstance.ApplicationPane() {

    private val historyPane = HistoryPane(session)
    private val phaserPane = DePhaserPane(session)
    private val networkOutputPane = NetworkOutputPane(session)
    private val noteOutputPane = NoteOutputPane(session)
    private val controlPane = ControlPane(this, session)

    override fun onCreate() {

        session.addOnEdited { setTitle() }

        val historyPanel = JPanel(GridLayout())
        historyPanel.border = BorderFactory.createEtchedBorder()
        historyPanel.add(historyPane)

        val phaserPanel = JPanel(GridLayout())
        phaserPanel.border = BorderFactory.createEtchedBorder()
        phaserPanel.add(phaserPane)

        val networkOutputPanel = JPanel(GridLayout())
        networkOutputPanel.border = BorderFactory.createEtchedBorder()
        networkOutputPanel.add(networkOutputPane)

        val noteOutputPanel = JPanel(GridLayout())
        noteOutputPanel.border = BorderFactory.createEtchedBorder()
        noteOutputPanel.add(noteOutputPane)

        val controlPanel = JPanel(GridLayout())
        controlPanel.border = BorderFactory.createEtchedBorder()
        controlPanel.add(controlPane)

        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.add(phaserPanel)
        topPanel.add(historyPanel)
        topPanel.add(networkOutputPanel)
        topPanel.add(noteOutputPanel)

        layout = BorderLayout()
        add(topPanel, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.SOUTH)

        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)) {
            session.stepCursor = session.correctedStepCursor - REGULAR_STEP_MOVE
        }
        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK)) {
            session.stepCursor = session.correctedStepCursor - SHIFT_STEP_MOVE
        }

        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK)) {
            session.clusterCursor = session.correctedClusterCursor - REGULAR_CLUSTER_MOVE
        }
        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK
                or InputEvent.ALT_DOWN_MASK)) {
            session.clusterCursor = session.correctedClusterCursor - SHIFT_CLUSTER_MOVE
        }

        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)) {
            session.stepCursor = session.correctedStepCursor + REGULAR_STEP_MOVE
        }
        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK)) {
            session.stepCursor = session.correctedStepCursor + SHIFT_STEP_MOVE
        }

        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK)) {
            session.clusterCursor = session.correctedClusterCursor + REGULAR_CLUSTER_MOVE
        }
        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK
                or InputEvent.ALT_DOWN_MASK)) {
            session.clusterCursor = session.correctedClusterCursor + SHIFT_CLUSTER_MOVE
        }

        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)) {
            session.stepCursor = 0
        }
        addSynchronizedSafeKeyListener(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)) {
            session.stepCursor = null
        }

    }

    private fun addSynchronizedSafeKeyListener(stroke: KeyStroke, action: (ActionEvent) -> (Unit)) {
        inputMap.put(stroke, stroke.hashCode())
        actionMap.put(stroke.hashCode(), object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                synchronized(session.recording) {
                    if (session.state == Session.SessionState.EDIT_SAFE)
                        action.invoke(e)
                }
            }
        })
    }

    override fun onPause() {

        AppInstance.title = core.FRAME_TITLE

    }

    override fun onResume() {

        setTitle()
        session.width = width
        session.clusterWidth = width.toDouble() / noteOutputPane.spacing

    }

    private fun setTitle() {

        AppInstance.title = "${session.recording.name}${if (session.isEdited) "*" else ""} - ${core.FRAME_TITLE}"

    }

    override fun onDestroy() {

        session.dispose()
        phaserPane.end()

    }

    override fun onClose() {

        val back = if (session.isEdited) {

            val options = arrayOf("Save", "Don't save", "Cancel")

            val choice = JOptionPane.showOptionDialog(AppInstance,
                    "Do you want to save your changes?",
                    "Save and Exit?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2])

            when (choice) {
                0 -> {
                    LoadingDialog(AppInstance, "Saving to file", "Saving") {

                        session.recording.save()

                    }
                    true
                }
                1 -> true
                else -> false
            }
        } else true

        if (back) AppInstance.popAll()

    }

    companion object {

        fun line(x1: Number, y1: Number, x2: Number, y2: Number): Line2D.Double =
                Line2D.Double(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        infix fun IntRange.overlap(other: IntRange): IntRange = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)

        infix fun ClosedFloatingPointRange<Double>.overlap(other: ClosedFloatingPointRange<Double>) = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)
        // These need to be two separate methods because the superclass both these types of range share, only store comparators
        // for if something is in range, thus you can't know the start and end values

        const val SHIFT_STEP_MOVE = 1
        const val SHIFT_CLUSTER_MOVE = 1

        const val REGULAR_STEP_MOVE = 15
        const val REGULAR_CLUSTER_MOVE = 5

    }

}