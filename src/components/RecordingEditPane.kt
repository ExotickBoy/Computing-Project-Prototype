package components

import core.MainApplication
import core.Model
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

    private val dePhaserView = DePhaserView(session)
    private val melOutputView = HistoryView(session, Model.MEL_BINS_AMOUNT.toDouble() * 2, true) { it.melImages }
    private val networkOutputView = HistoryView(session, Model.PITCH_RANGE.toDouble(), false) { it.noteImages }
    private val noteOutputView = NoteOutputView(session)
    private val controlPane = ControlPane(application, scene, session)


    private val dePhaserViewLabel = Label(DE_PHASED_LABEL)
    private val melOutputViewLabel = Label(SPECTROGRAM_LABEL)
    private val netWorkOutputViewLabel = Label(NETWORK_OUTPUT_LABEL)

    override fun onCreate(): Scene {

        session.addOnEdited {
            setTitle()
        }

        root.children.addAll(
                dePhaserViewLabel,
                dePhaserView,
                melOutputViewLabel,
                melOutputView)

        if (SHOW_NETWORK_OUTPUT)
            root.children.addAll(
                    netWorkOutputViewLabel,
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

    /**
     * This method adds a key listener to the scene.  The runnable will run with recording synchronized
     * and only when the session is edit safe
     */
    private fun addKeyListener(keyCombination: KeyCombination, runnable: Runnable) {
        scene.accelerators[keyCombination] = Runnable {
            synchronized(session.recording) {
                if (session.state == Session.SessionState.EDIT_SAFE) {
                    runnable.run()
                }
            }
        }
    }

    override fun onPause() {

        setTitle(MainApplication.TITLE)

    }

    override fun onResume() {

        // adds the name of the current recording to the title of the frame
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
        dePhaserView.end()

    }

    override fun onClose() {

        // attempt to save changes when the user requests to close

        session.pausePlayback()
        session.pauseRecording()

        when {
            session.state == Session.SessionState.EDIT_SAFE && session.isEdited -> {

                val choice = RecordingEditPane.showSaveDialog()

                if (choice.get().buttonData == ButtonBar.ButtonData.YES) {

                    val dialog = LoadingDialog(SAVE_DIALOG_TEXT, SAVE_DIALOG_TITLE)
                    Platform.runLater {
                        session.recording.save()
                        dialog.dispose()
                        application.popAll()
                    }

                } else if (choice.get().buttonData == ButtonBar.ButtonData.NO) application.popAll()

            }
            session.state == Session.SessionState.GATHERING -> session.pauseRecording()
            session.state == Session.SessionState.PLAYING_BACK -> session.pausePlayback()
            session.state == Session.SessionState.EDIT_SAFE -> application.popAll()
        }


    }

    companion object {

        infix fun ClosedFloatingPointRange<Double>.overlap(other: ClosedFloatingPointRange<Double>) = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)
        // These need to be two separate methods because the superclass both these types of range share, only store comparators
        // for if something is in range, thus you can't know the start and end values

        /* These are how much the cursors change when using hot keys*/
        const val SHIFT_STEP_MOVE = 1
        const val SHIFT_CLUSTER_MOVE = 1

        const val REGULAR_STEP_MOVE = 15
        const val REGULAR_CLUSTER_MOVE = 5

        const val SHOW_NETWORK_OUTPUT = true

        const val RECORDING_EDIT_PANE_WIDTH = 500.0

        private const val DE_PHASED_LABEL = "Visualisation"
        private const val SPECTROGRAM_LABEL = "Frequency Analysis"
        private const val NETWORK_OUTPUT_LABEL = "Raw Network Output"

        private const val SAVE_DIALOG_TEXT = "Saving to file"
        private const val SAVE_DIALOG_TITLE = "Saving"

        /**
         * This shows the dialog that asks the user if they want to 'save', 'don't save' or 'cancel'
         * @return an optional pressed button
         */
        fun showSaveDialog(): Optional<ButtonType> {

            val alert = Alert(Alert.AlertType.WARNING)
            if (MainApplication.icon != null)
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