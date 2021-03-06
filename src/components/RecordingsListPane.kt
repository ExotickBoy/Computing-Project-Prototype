package components

import core.MainApplication
import core.Recording
import core.Session
import dialogs.LoadingDialog
import dialogs.NewRecordingDialog
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.Mnemonic
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import java.io.File
import java.io.FileInputStream
import kotlin.math.roundToInt

/**
 * This is the activity that the user starts on.  It consists of a list of recent recordings, and controls to make new
 * ones or to edit the existing ones
 *
 * @author Kacper Lubisz
 *
 */
class RecordingsListPane(application: MainApplication) : MainApplication.Activity(application) {

    private val recordings: MutableList<Recording.PossibleRecording> = mutableListOf()
    private lateinit var recordingList: ListView<Recording.PossibleRecording>

    private val root = VBox()
    private val repaintThread = RepaintThread(root)

    override fun onCreate(): Scene {

        val root = VBox()
        val scene = Scene(root)

        val newButton = Button(NEW_RECORDING_BUTTON_TEXT)
        newButton.setFocusMnemonic(NEW_BUTTON_MENMNIC, scene)

        val editButton = Button(EDIT_BUTTON_TEXT)
        editButton.setFocusMnemonic(EDIT_BUTTON_MNEMONIC, scene)
        editButton.isDisable = true


        val deleteButton = Button(DELETE_BUTTON_TEXT)
        deleteButton.setFocusMnemonic(DELETE_BUTTON_MENMONIC, scene)
        deleteButton.isDisable = true

        val placeholderLabel = Label(NO_RECORDINGS_PROMPT)
        placeholderLabel.textAlignment = TextAlignment.CENTER

        recordingList = ListView()
        recordingList.placeholder = placeholderLabel
        recordingList.selectionModel.selectionMode = SelectionMode.MULTIPLE
        recordingList.setCellFactory {
            ListItem()
        }
        recordingList.setOnMouseClicked {
            if (it.clickCount == 2) { // double clicked
                edit()
                it.consume()
            }
        }
        recordingList.selectionModel.selectedIndexProperty().addListener({ _ ->
            deleteButton.isDisable = recordingList.selectionModel.selectedIndices.isEmpty()
            editButton.isDisable = recordingList.selectionModel.selectedIndices.size != 1
        })
        recordingList.setOnKeyPressed {
            when {
                it.code == KeyCode.ENTER -> {
                    edit()
                    it.consume()
                }
                it.code == KeyCode.DELETE -> {
                    delete()
                    it.consume()
                }

            }
        }




        newButton.setOnAction {
            NewRecordingDialog(application, recordings)
        }
        deleteButton.setOnAction {

            val alert = Alert(AlertType.WARNING)
            alert.title = DELETE_DIALOG_TITLE
            if (MainApplication.icon != null)
                (alert.dialogPane.scene.window as Stage).icons.add(MainApplication.icon)
            alert.headerText = DELETE_DIALOG_MESSAGE + if (recordingList.selectionModel.selectedIndices.size == 1) "" else "s" +
                    "\n${recordingList.selectionModel.selectedIndices.map {
                        recordings[it].metaData.name
                    }.reduce { acc, s -> "$acc, ${if (acc.length - acc.lastIndexOf("\n") >= 30) "\n" else ""}$s" }}?"

            val deleteButtonType = ButtonType(DELETE_DIALOG_DELETE_TEXT, ButtonBar.ButtonData.YES)
            val cancelButtonType = ButtonType(DELETE_DIALOG_CANCEL_TEXT, ButtonBar.ButtonData.CANCEL_CLOSE)

            alert.buttonTypes.setAll(deleteButtonType, cancelButtonType)

            val result = alert.showAndWait()
            if (result.get() == deleteButtonType) { // delete pressed
                delete()
            }

        }
        editButton.setOnAction {
            edit()
        }

        val recordingLabel = Label(RECENT_RECORDINGS_LABEL_TEXT)
        recordingLabel.setFocusMnemonic(RECOENT_RECORDINGS_LABEL_MENMONIC, scene)
        recordingLabel.labelFor = recordingList

        // layout

        val buttonPanel = HBox()
        buttonPanel.spacing = SPACING
        buttonPanel.isCenterShape = true
        HBox.setHgrow(newButton, Priority.ALWAYS)
        newButton.maxWidth = Double.MAX_VALUE
        buttonPanel.children.addAll(newButton, editButton, deleteButton)

        val listPanel = VBox()
        listPanel.children.addAll(recordingLabel, recordingList)
        listPanel.padding = Companion.makeInsets(top = SPACING)

        root.children.addAll(buttonPanel, listPanel)
        root.maxHeight = Double.MAX_VALUE
        root.padding = Insets(PADDING)

        return scene

    }

    /**
     * This deletes the selected activities
     */
    private fun delete() {
        recordingList.selectionModel.selectedIndices.sortedDescending().forEach {
            recordings[it].file.delete()
            recordings.removeAt(it)
            recordingList.items.removeAt(it)
        }
    }

    /**
     * This method starts a new activity that allows the usre to edit their activity
     */
    private fun edit() {
        if (recordingList.selectionModel.selectedIndex != -1) {

//            val start = System.currentTimeMillis()

            val possibleRecording = recordings[recordingList.selectionModel.selectedIndex]

            val dialog = LoadingDialog(LOADING_DIALOG_TEXT_PREFIX + possibleRecording.metaData.name, LOADING_DIALOG_TITLE)
            Platform.runLater {
                val session = Session(Recording.deserialize(FileInputStream(possibleRecording.file)))
                dialog.dispose()
                application.push(RecordingEditPane(session, application))

//                println("loading -> ${System.currentTimeMillis() - start}ms")

            }

        }

    }

    override fun onPause() {
        repaintThread.isPaused = true
    }

    override fun onResume() {

        // loads the recordings each time it is resumed

        repaintThread.isPaused = false

        recordings.clear()
        recordingList.selectionModel.clearSelection()
        recordingList.items.clear()

        recordings.addAll(Recording.findPossibleRecordings(File(Recording.DEFAULT_SAVE_PATH)))
        recordings.sortByDescending { it.metaData.lastEdited }
        recordings.forEach { recordingList.items.add(it) }

        recordingList.refresh()

    }

    override fun onDestroy() {}

    override fun onClose() {
        application.popAll()
    }

    /**
     * This thread is responsible for repainting the scene roughly 10 times per second.
     * This is so that the relative time labels stay consistent
     */
    private class RepaintThread(val root: VBox) : Thread(REPAINT_THREAD_TITLE) {


        init {
            start()
        }

        var isPaused = false

        override fun run() {

            while (!isInterrupted) {
                if (isPaused)
                    while (isPaused) {
                        onSpinWait()
                    }
                else {
                    root.layout()
                    sleep((1000.0 / REFRESH_RATE).toLong())
                }
            }

        }

        companion object {
            const val REFRESH_RATE = 10 // 10 Hz
        }

    }

    companion object {

        private const val NEW_RECORDING_BUTTON_TEXT = "New Recording"
        private const val EDIT_BUTTON_TEXT = "Edit"
        private const val DELETE_BUTTON_TEXT = "Delete"
        private const val NO_RECORDINGS_PROMPT = "Press '$NEW_RECORDING_BUTTON_TEXT'\n to make a new recording"
        private const val RECENT_RECORDINGS_LABEL_TEXT = "Recent Recordings:"

        private const val DELETE_DIALOG_DELETE_TEXT = "Delete"
        private const val DELETE_DIALOG_CANCEL_TEXT = "Cancel"
        private const val DELETE_DIALOG_TITLE = "Delete Confirmation"
        private const val DELETE_DIALOG_MESSAGE = "Are you sure you want to delete recording"

        private const val LOADING_DIALOG_TEXT_PREFIX = "Loading "
        private const val LOADING_DIALOG_TITLE = "Loading"

        private const val NEW_BUTTON_MENMNIC = "N"
        private const val EDIT_BUTTON_MNEMONIC = "E"
        private const val DELETE_BUTTON_MENMONIC = "D"
        private const val RECOENT_RECORDINGS_LABEL_MENMONIC = "R"

        private const val REPAINT_THREAD_TITLE = "Repaint Thread"

        private const val SPACING = 5.0
        private const val PADDING = 10.0


        /**
         * Convenience function for creating insets which are used when adding paddign
         */
        fun makeInsets(top: Number = 0, right: Number = 0, bottom: Number = 0, left: Number = 0): Insets = Insets(top.toDouble(), right.toDouble(), bottom.toDouble(), left.toDouble())

        /**
         * This adds a mnemonic to a node(component). This means that when the key is pressed the node will become focused
         * @param key the string representing the key
         * @param scene the scene that the hot key will be added to
         *
         */
        fun Node.setFocusMnemonic(key: String, scene: Scene) {
            scene.addMnemonic(Mnemonic(this, KeyCodeCombination(KeyCode.valueOf(key), KeyCombination.ALT_DOWN)))
        }

    }

    /**
     * This object will create the cells in the recording list view
     */
    private class ListItem : ListCell<Recording.PossibleRecording>() {

        /**
         * This is called to update or create the cells of the list.
         * The ListItem consists of the name on the left side and the length and relative time on the right
         */
        public override fun updateItem(item: Recording.PossibleRecording?, empty: Boolean) {

            super.updateItem(item, empty)

            graphic = if (item != null) {

                val timeLabel = Text(item.metaData.created.toRelativeTime())
                val lengthLabel = Text(item.metaData.length.toLength())
                val nameLabel = Text(item.metaData.name)

                nameLabel.font = Font.font(nameLabel.font.name, 20.0)

                val propertyVBox = VBox(timeLabel, lengthLabel)
                val root = BorderPane()
                root.right = propertyVBox
                root.left = nameLabel

                root

            } else null


        }

        /**
         * Creates a string that describes how long a recording is
         * @return the string
         */
        private fun Double.toLength(): String {
            val minutes = this / 60
            return when {
                this == 0.0 -> "-"
                this < 1 -> "< 1s"
                this < 60 -> "${this.roundToInt()}s"
                minutes < 5 -> "${minutes.roundToInt()}m"
                else -> "${minutes.roundToInt()}m ${this.rem(60).roundToInt()}s"
            }
        }

        /**
         * Creates a string that describes how long in the past a time stamp is
         * @return the description
         */
        private fun Long.toRelativeTime(): String {
            val now = System.currentTimeMillis()
            val millis = now - this
            val seconds = millis / 1000.0
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7

            return when {
                seconds < 30 -> "just now"
                seconds < 60 -> "${seconds.roundToInt()} seconds ago"
                minutes < 2 -> "1 minute ago"
                minutes < 60 -> "${minutes.roundToInt()} minutes ago"
                hours < 2 -> "1 hour ago"
                hours < 24 -> "${hours.roundToInt()} hours ago"
                days < 2 -> "1 day ago"
                days < 7 -> "${days.roundToInt()} days ago"
                weeks < 2 -> "1 week ago"
                weeks < 52 -> "${weeks.roundToInt()} weeks ago"
                else -> "over a year ago"
            }
        }

    }

}
