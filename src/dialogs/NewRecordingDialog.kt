package dialogs

import components.RecordingEditPane
import components.RecordingsListPane.Companion.makeInsets
import components.RecordingsListPane.Companion.setFocusMnemonic
import core.*
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

class NewRecordingDialog(private val application: MainApplication, private val recordings: MutableList<Recording.PossibleRecording>) {

    private var customTuning: Tuning? = null
    private val stage: Stage = Stage()

    private val nameLabel: Label
    private val nameField: TextField
    private val tuningLabel: Label
    private var tuningComboBox: ComboBox<String>

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
        tuningComboBox.setOnAction {
            if (tuningComboBox.selectionModel.selectedIndex == tuningComboBox.items.lastIndex) {
//                tuningComboBox.transferFocus() TODO
//                TuningMakerDialog(this@NewRecordingDialog, customTuning)
            }
        }
        tuningComboBox.setFocusMnemonic("T", scene)

        loadButton = Button("Load File")
        loadButton.setFocusMnemonic("L", scene)
        loadButton.setOnAction {
            load()
        }

        recordButton = Button("Record")
        recordButton.setFocusMnemonic("R", scene)
        recordButton.setOnAction {
            record(nameField, recordings)
        }
        nameField.setOnAction {
            record(nameField, recordings)
        }

        nameLabel = Label("Name:")
        nameLabel.labelFor = nameField

        tuningLabel = Label("Tuning:")
        tuningLabel.labelFor = tuningComboBox

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

    private fun load() {

        val fileChooser = FileChooser()
        fileChooser.title = "Open Resource File"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("WAV(16 bit PMC)", "*.wav"))
        fileChooser.initialDirectory = File("").absoluteFile
        // this makes the directory start in the same folder as the location of the executable
        val result: File? = fileChooser.showOpenDialog(stage)

        if (result != null) {

            val name = if (nameField.text.isEmpty()) result.name.substring(0, result.name.length - 4)  else nameField.text
            val regex = "$name(\\d| )*".toRegex()
            val sameNames = recordings.map { it.metaData.name }
                    .filter { regex.matches(it) }
                    .map { if (it.length == name.length) 0 else it.substring(name.length).trim().toInt() }
                    .max()
            val newName = name + if (sameNames == null) "" else " ${sameNames + 1}"

            val tuning = if (tuningComboBox.selectionModel.selectedIndex == Tuning.DEFAULT_TUNINGS.size)
                customTuning ?: Tuning.DEFAULT_TUNINGS[0] // this null case shouldn't happen
            else
                Tuning.DEFAULT_TUNINGS[tuningComboBox.selectionModel.selectedIndex]

            val recording = Recording(tuning, newName)
            val reader = SoundFileReader(recording, result)

            try {


                reader.open()
                val dialog = LoadingDialog("Reading ${result.name}", "Reading from file")
                reader.start()
                reader.join()
                dialog.dispose()

                val session = Session(recording)
                session.stepCursor = null
                session.onEdited()
                stage.close()
                application.push(RecordingEditPane(session, application))

            } catch (e: Exception) {

                val alert = Alert(Alert.AlertType.ERROR)
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

    private fun record(nameField: TextField, recordings: MutableList<Recording.PossibleRecording>) {
        val name = if (nameField.text.isEmpty()) "Nameless" else nameField.text
        val regex = "$name(\\d| )*".toRegex()
        val sameNames = recordings.map { it.metaData.name }
                .filter { regex.matches(it) }
                .map { if (it.length == name.length) 0 else it.substring(name.length).trim().toInt() }
                .max()
        val newName = name + if (sameNames == null) "" else " ${sameNames + 1}"

        val tuning = if (tuningComboBox.selectionModel.selectedIndex == Tuning.DEFAULT_TUNINGS.size)
            customTuning ?: Tuning.DEFAULT_TUNINGS[0] // this null case shouldn't happen
        else
            Tuning.DEFAULT_TUNINGS[tuningComboBox.selectionModel.selectedIndex]

        val session = Session(Recording(tuning, newName))
        application.push(RecordingEditPane(session, application))
        stage.close()

    }

    fun refresh(newTuning: Tuning?) {

        if (customTuning == null) {
            if (newTuning == null) {

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
