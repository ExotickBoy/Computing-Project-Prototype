package dialogs

import components.RecordingEditPane
import components.RecordingsListPane.Companion.makeInsets
import components.RecordingsListPane.Companion.setFocusMnemonic
import core.*
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.File
import java.io.IOException

/**
 * This class creates the dialog that allows the user to create a new recording.
 * This dialog consists of: name field, tuning combo box, load from file button and record button
 *
 * @author Kacper Lubisz
 *
 * @property application  The instance of MainApplication that is required to allow this dialog to add put activities onto the activity stack
 * @property recordings The metadata of all the possible recordings the app can read from, this is used to make sure that a name isn't used twice
 *
 * @see MainApplication
 * @see Recording.PossibleRecording
 *
 */
class NewRecordingDialog(private val application: MainApplication, private val recordings: MutableList<Recording.PossibleRecording>) {

    // The window
    private val stage: Stage = Stage()

    // the custom tuning that could potentially be used
    private var customTuning: Tuning? = null

    private val nameLabel: Label
    private val nameField: TextField
    private val tuningLabel: Label
    private val tuningComboBox: ComboBox<String>

    private val recordButton: Button
    private val loadButton: Button

    init {

        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "New Recording"
        if (MainApplication.icon != null)
            stage.icons.add(MainApplication.icon)

        val root = BorderPane()
        val scene = Scene(root)

        nameField = TextField()
        nameField.promptText = "Recording Name"
        nameField.setFocusMnemonic("N", scene)

        val tunings = Tuning.DEFAULT_TUNINGS.map { it.name }.toMutableList()
        tunings.add(MAKE_TUNING_TEXT)
        tuningComboBox = ComboBox()
        tuningComboBox.items.addAll(tunings.toTypedArray())
        tuningComboBox.selectionModel.select(0)
        tuningComboBox.selectionModel.selectedIndexProperty().addListener { _ ->
            if (tuningComboBox.selectionModel.selectedIndex == tuningComboBox.items.lastIndex) {
                TuningMakerDialog(this@NewRecordingDialog, customTuning)
            }
        }
        tuningComboBox.setFocusMnemonic("T", scene)
        // the last value of the combo box will be create/edit custom recording.
        // If a custom tuning exists the second to bottom will be that recording

        loadButton = Button("Load File")
        loadButton.setFocusMnemonic("L", scene)
        loadButton.setOnAction {
            load()
        }

        recordButton = Button("Record")
        recordButton.setFocusMnemonic("R", scene)
        recordButton.setOnAction {
            record()
        }
        nameField.setOnAction {
            record()
        }

        nameLabel = Label("Name:")
        nameLabel.labelFor = nameField

        tuningLabel = Label("Tuning:")
        tuningLabel.labelFor = tuningComboBox

        // layout

        val grid = GridPane()

        tuningComboBox.maxWidth = Double.MAX_VALUE
        nameField.maxWidth = Double.MAX_VALUE

        grid.add(nameLabel, 0, 0)
        grid.add(nameField, 1, 0)
        grid.add(tuningLabel, 0, 1)
        grid.add(tuningComboBox, 1, 1)
        grid.hgap = 5.0
        grid.vgap = 5.0

        val buttons = HBox(5.0)
        buttons.children.addAll(loadButton, recordButton)
        buttons.padding = makeInsets(top = 10)
        buttons.alignment = Pos.CENTER

        root.center = grid
        root.bottom = buttons
        root.padding = Insets(10.0)

        stage.scene = scene
        stage.showAndWait()

    }

    /**
     * This method is the routine for loading a recording from a file
     */
    private fun load() {

        // the file chooser allows the user to choose a file on their computer
        val fileChooser = FileChooser()
        fileChooser.title = "Open"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("WAV(16 bit PMC)", "*.wav"))
        fileChooser.initialDirectory = File("").absoluteFile
        // this makes the directory start in the same folder as the location of the executable
        val result: File? = fileChooser.showOpenDialog(stage)

        if (result != null) {

            // if no name is specified it will use the name of the file read from
            val name = if (nameField.text.isEmpty()) result.name.substring(0, result.name.length - 4) else nameField.text
            val newName = getName(name)

            val tuning = if (tuningComboBox.selectionModel.selectedIndex == Tuning.DEFAULT_TUNINGS.size)
                customTuning ?: Tuning.DEFAULT_TUNINGS[0] // this null case shouldn't happen
            else
                Tuning.DEFAULT_TUNINGS[tuningComboBox.selectionModel.selectedIndex]

            val recording = Recording(tuning, newName)
            val reader = SoundFileReader(recording, result)

            try {

                reader.open()
                val dialog = LoadingDialog("Reading ${result.name}", "Reading from file")
                Platform.runLater {
                    // this doesn't cause a delay.  The idea of running this later is that otherwise the loading dialog
                    // won't ever repaint.  This is because the process of repainting the dialog is enqueued to happen
                    // after handling this button press is over.  Because of this it means that it won't repaint until
                    // after it has already been disposed off. This doesn't cause a crash but means that the dialog
                    // is empty during the time it us visible.
                    // The solution to this is to enqueue the rest of this handling to happen after repainting

                    reader.start() // reads the file
                    reader.join()
                    dialog.dispose()

                    val session = Session(recording)
                    session.stepCursor = null
                    session.onEdited()
                    stage.close()
                    application.push(RecordingEditPane(session, application))
                    // changes the content of the main stage

                }
            } catch (e: Exception) { // when loading fails

                val alert = Alert(Alert.AlertType.ERROR)
                if (MainApplication.icon != null)
                    (alert.dialogPane.scene.window as Stage).icons.add(MainApplication.icon)
                alert.title = "Error"
                alert.headerText = "An error occurred"
                alert.contentText = when (e) {
                    is javax.sound.sampled.UnsupportedAudioFileException -> "This file format isn't supported"
                    is SoundFileReader.UnsupportedBitDepthException -> "Only 16 bit depth supported"
                    is SoundFileReader.UnsupportedChannelsException -> "Only mono supported"
                    is IOException -> "Read error occurred"
                    else -> "Unknown error occurred ${e.message}"
                }

                alert.showAndWait()

            }

        }
    }

    /**
     * This method is the routine for creating a new recording
     */
    private fun record() {

        val name = if (nameField.text.isEmpty()) Recording.DEFAULT_NAME else nameField.text
        val newName = getName(name)

        val tuning = if (tuningComboBox.selectionModel.selectedIndex == Tuning.DEFAULT_TUNINGS.size)
            customTuning ?: Tuning.DEFAULT_TUNINGS[0] // this null case shouldn't happen
        else
            Tuning.DEFAULT_TUNINGS[tuningComboBox.selectionModel.selectedIndex]

        val session = Session(Recording(tuning, newName))
        application.push(RecordingEditPane(session, application))
        stage.close()

    }

    /**
     * This method uses regex to find all (if any) the recordings with the same name as the one specified.
     * It either leaves the name unchanged or adds a suffix number to it to make the name unique
     */
    private fun getName(name: String): String {

        val regex = "$name(\\d| )*".toRegex()
        val sameNames = recordings.map { it.metaData.name }
                .filter { regex.matches(it) }
                .map { if (it.length == name.length) 0 else it.substring(name.length).trim().toInt() }
                .max()

        return name + if (sameNames == null) "" else " ${sameNames + 1}"

    }

    /**
     * This method is for being called by the tuning editor dialog.
     * It allows for handling the creation of a new tuning
     */
    fun refresh(newTuning: Tuning?) {

        if (customTuning == null) { // there isn't already a custom tuning

            if (newTuning == null) { // if a new tuning was created

                tuningComboBox.selectionModel.select(0)

            } else { // added

                tuningComboBox.items[tuningComboBox.items.lastIndex] = newTuning.name
                tuningComboBox.items.add("Edit ${newTuning.name}")

                // the new one will already be selected
            }

        } else {

            if (newTuning == null) {

                tuningComboBox.selectionModel.select(0)
                tuningComboBox.items.removeAt(tuningComboBox.items.lastIndex)
                tuningComboBox.items[tuningComboBox.items.lastIndex] = MAKE_TUNING_TEXT


            } else { // changed

                tuningComboBox.items[tuningComboBox.items.lastIndex - 1] = newTuning.name
                tuningComboBox.items[tuningComboBox.items.lastIndex] = "Edit ${newTuning.name}"

                tuningComboBox.selectionModel.select(tuningComboBox.items.lastIndex - 1)

            }
        }

        customTuning = newTuning

    }

    companion object {

        const val MAKE_TUNING_TEXT = "Create New Tuning"

    }

}
