package components

import core.MainApplication
import core.Session
import dialogs.LoadingDialog
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.util.*
import kotlin.math.max
import kotlin.math.min


class RecordingEditPane(val session: Session, application: MainApplication) : MainApplication.Activity(application) {

    private val root = VBox()
    private val scene = Scene(root)

    private val historyView = HistoryView(session)
    private val phaserView = DePhaserView(application, session)
    private val networkOutputView = NetworkOutputView(session)
    private val noteOutputView = NoteOutputView(session)

    private val controlPane = ControlPane(application, scene, session)

    override fun onCreate(): Scene {

        session.addOnEdited {
            setTitle()
        }
        root.children.addAll(
                Label("Visualisation"),
                phaserView,
                Label("Frequency Analysis"),
                historyView)

        if (SHOW_NETWORK_OUTPUT)
            root.children.addAll(
                    Label("Raw Network Output"),
                    networkOutputView)

        root.children.addAll(
                noteOutputView,
                controlPane
        )

        // shift means move slowly
        // alt means move cluster

        addKeyListener(KeyCodeCombination(KeyCode.LEFT), Runnable {
            session.stepCursor = session.correctedStepCursor - REGULAR_STEP_MOVE
        })
        addKeyListener(KeyCodeCombination(KeyCode.LEFT, KeyCombination.SHIFT_DOWN), Runnable {
            session.stepCursor = session.correctedStepCursor - SHIFT_STEP_MOVE
        })
        addKeyListener(KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN), Runnable {
            session.clusterCursor = session.correctedClusterCursor - REGULAR_CLUSTER_MOVE
        })
        addKeyListener(KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN), Runnable {
            session.clusterCursor = session.correctedClusterCursor - SHIFT_CLUSTER_MOVE
        })

        addKeyListener(KeyCodeCombination(KeyCode.RIGHT), Runnable {
            session.stepCursor = session.correctedStepCursor + REGULAR_STEP_MOVE
        })
        addKeyListener(KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHIFT_DOWN), Runnable {
            session.stepCursor = session.correctedStepCursor + SHIFT_STEP_MOVE
        })
        addKeyListener(KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN), Runnable {
            session.clusterCursor = session.correctedClusterCursor + REGULAR_CLUSTER_MOVE
        })
        addKeyListener(KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN), Runnable {
            session.clusterCursor = session.correctedClusterCursor + SHIFT_CLUSTER_MOVE
        })

        addKeyListener(KeyCodeCombination(KeyCode.HOME), Runnable { session.stepCursor = 0 })
        addKeyListener(KeyCodeCombination(KeyCode.END), Runnable { session.stepCursor = null })

        return scene

    }

    private fun addKeyListener(keyCombination: KeyCombination, runnable: Runnable) {
        synchronized(session.recording) {
            if (session.state == Session.SessionState.EDIT_SAFE) {
                scene.accelerators[keyCombination] = runnable
            }
        }
    }

    override fun onPause() {

        setTitle(MainApplication.TITLE)

    }

    override fun onResume() {

        setTitle()
        session.width = root.width.toInt()
        session.clusterWidth = root.width / noteOutputView.spacing

    }

    private fun setTitle(title: String? = null) {
        Platform.runLater {
            application.setTitle(title ?: session.recording.name+(if (session.isEdited) "*" else "")+" - "
            +MainApplication.TITLE)
        }
    }

    override fun onDestroy() {

        session.dispose()
        phaserView.end()

    }

    override fun onClose() {

        val back = if (session.isEdited) {

            val choice = RecordingEditPane.showSaveDialog()

            when (choice.get().buttonData) {
                ButtonBar.ButtonData.YES -> {
                    val dialog = LoadingDialog("Saving to file", "Saving")
                    session.recording.save()
                    dialog.dispose()
                    true
                }
                ButtonBar.ButtonData.NO -> true
                else -> false
            }
        } else true

        if (back) application.popAll()

    }

    companion object {

        infix fun IntRange.overlap(other: IntRange): IntRange = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)

        infix fun ClosedFloatingPointRange<Double>.overlap(other: ClosedFloatingPointRange<Double>) = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)
        // These need to be two separate methods because the superclass both these types of range share, only store comparators
        // for if something is in range, thus you can't know the start and end values

        const val SHIFT_STEP_MOVE = 1
        const val SHIFT_CLUSTER_MOVE = 1

        const val REGULAR_STEP_MOVE = 15
        const val REGULAR_CLUSTER_MOVE = 5

        const val SHOW_NETWORK_OUTPUT = true

        fun showSaveDialog(): Optional<ButtonType> {

            val alert = Alert(Alert.AlertType.WARNING)
            (alert.dialogPane.scene.window as Stage).icons.add(MainApplication.icon)
            alert.title = "Save"
            alert.headerText = "Do you want to save your changes?"

            val saveButtonType = ButtonType("Save", ButtonBar.ButtonData.YES)
            val notSaveButtonType = ButtonType("Don't Save", ButtonBar.ButtonData.NO)
            val cancelButtonType = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)

            alert.buttonTypes.setAll(saveButtonType, notSaveButtonType, cancelButtonType)

            return alert.showAndWait()

        }

    }

}