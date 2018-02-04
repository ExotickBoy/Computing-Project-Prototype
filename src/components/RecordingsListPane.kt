package components

import core.MainApplication
import core.Recording
import core.Session
import dialogs.LoadingDialog
import dialogs.NewRecordingDialog
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
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
import java.io.File
import java.io.FileInputStream
import javax.swing.JOptionPane
import kotlin.math.roundToInt


class RecordingsListPane(application: MainApplication) : MainApplication.Activity(application) {

    private val recordings: MutableList<Recording.PossibleRecording> = mutableListOf()
    private lateinit var recordingList: ListView<Recording.PossibleRecording>

    private val repaintThread = RepaintThread(this)

    override fun onCreate(): Scene {

        val root = BorderPane()
        val scene = Scene(root)

        val newButton = Button("New Recording")
        newButton.setFocusMnemonic("N", scene)

        val editButton = Button("Edit")
        editButton.setFocusMnemonic("E", scene)
        editButton.isDisable = true

        val deleteButton = Button("Delete")
        deleteButton.setFocusMnemonic("D", scene)
        deleteButton.isDisable = true

        recordingList = ListView()
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
        recordingList.selectionModel.selectedIndexProperty().addListener({ observable ->
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
            NewRecordingDialog(recordings)
        }
        deleteButton.setOnAction {

            val choice = JOptionPane.showOptionDialog(null,
                    "Are you sure you want to delete recording" +
                            "${if (recordingList.selectionModel.selectedIndices.size == 1) "" else "s"} " +
                            "\n${recordingList.selectionModel.selectedIndices.map {
                                recordings[it].metaData.name
                            }.reduce { acc, s -> "$acc, ${if (acc.length - acc.lastIndexOf("\n") >= 30) "\n" else ""}$s" }}?",
                    "Delete Confirmation",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    listOf("Delete", "No").toTypedArray(),
                    0)

            if (choice == 0)  // delete pressed
                delete()

        }
        editButton.setOnAction {
            edit()
        }
        val recordingLabel = Label("Recent Recordings:")
        recordingLabel.setFocusMnemonic("R", scene)
        recordingLabel.labelFor = recordingList

        val buttonPanel = HBox()
        buttonPanel.spacing = 5.0
        buttonPanel.isCenterShape = true
        HBox.setHgrow(newButton, Priority.ALWAYS)
        newButton.maxWidth = Double.MAX_VALUE
        buttonPanel.children.addAll(newButton, editButton, deleteButton)

        val listPanel = VBox()
        listPanel.children.addAll(recordingLabel, recordingList)
        listPanel.padding = Companion.makeInsets(top = 5.0)

        root.top = buttonPanel
        root.center = listPanel
        root.padding = Insets(10.0)

        return scene

    }

    private fun delete() {
        recordingList.selectionModel.selectedIndices.sortedDescending().forEach {
            recordings[it].file.delete()
            recordings.removeAt(it)
            recordingList.items.removeAt(it)
        }
    }

    private fun edit() {
        if (recordingList.selectionModel.selectedIndex != -1) {

//            val start = System.currentTimeMillis()

            val possibleRecording = recordings[recordingList.selectionModel.selectedIndex]
            val dialog = LoadingDialog("Loading ${possibleRecording.metaData.name}", "Loading")
            val session = Session(Recording.deserialize(FileInputStream(possibleRecording.file)))
            dialog.dispose()

            application.push(RecordingEditPane(session, application))

//            println("loading -> ${System.currentTimeMillis() - start}ms")

        }
    }

    override fun onPause() {
        repaintThread.isPaused = true
    }

    override fun onResume() {
        repaintThread.isPaused = false

        recordings.clear()
        recordings.addAll(Recording.findPossibleRecordings(File(Recording.DEFAULT_PATH)))
        recordings.sortByDescending { it.metaData.lastEdited }

        recordings.forEach { recordingList.items.add(it) }

    }

    override fun onDestroy() {
    }

    override fun onClose() {
        application.popAll()
    }

    private class RepaintThread(val pane: RecordingsListPane) : Thread("Repaint Thread") {

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
//                    pane.repaint()
                    sleep(100)
                }
            }

        }

    }

    companion object {
        fun makeInsets(top: Number = 0, right: Number = 0, bottom: Number = 0, left: Number = 0): Insets = Insets(top.toDouble(), right.toDouble(), bottom.toDouble(), left.toDouble())

        private fun Node.setFocusMnemonic(key: String, scene: Scene) {
            scene.addMnemonic(Mnemonic(this, KeyCodeCombination(KeyCode.valueOf(key), KeyCombination.ALT_ANY)))
        }

    }

    internal class ListItem : ListCell<Recording.PossibleRecording>() {

        public override fun updateItem(item: Recording.PossibleRecording?, empty: Boolean) {

            super.updateItem(item, empty)

            if (item != null) {
                val timeLabel = Text(item.metaData.created.toRelativeTime())
                val lengthLabel = Text(item.metaData.length.toLength())
                val nameLabel = Text(item.metaData.name)

                nameLabel.font = Font.font(nameLabel.font.name, 20.0)

                val propertyVBox = VBox(timeLabel, lengthLabel)
                val root = BorderPane()
                root.right = propertyVBox
                root.left = nameLabel

                graphic = root
            }

        }

        private fun Double.toLength(): String {
            val minutes = this / 60
            return when {
                this == 0.0 -> "-"
                this < 60 -> "${this.roundToInt()}s"
                minutes < 5 -> "${minutes.roundToInt()}m"
                else -> "${minutes.roundToInt()}m ${this.rem(60).roundToInt()}s"
            }
        }

        private fun Long.toRelativeTime(): String {
            val now = System.currentTimeMillis()
            val millis = now - this
            val seconds = millis / 1000.0
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7

            return when {
                seconds < 10 -> "just now"
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
