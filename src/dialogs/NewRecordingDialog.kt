package dialogs

import components.RecordingsListPane.Companion.makeInsets
import components.RecordingsListPane.Companion.setFocusMnemonic
import core.MainApplication
import core.Recording
import core.Session
import core.Tuning
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.*
import javafx.stage.Modality
import javafx.stage.Stage


class NewRecordingDialog(recordings: MutableList<Recording.PossibleRecording>) {

    private var customTuning: Tuning? = null
    private var tuningComboBox: ComboBox<String>

    private val stage: Stage

    init {

        stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "New Recording"
        if (MainApplication.icon != null)
            stage.icons.add(MainApplication.icon)

        val root = BorderPane()
        val scene = Scene(root)

        val nameField = TextField()
        nameField.setFocusMnemonic("N", scene)

        val tunings = Tuning.DEFAULT_TUNINGS.map { it.name }.toMutableList()
        tunings.add("Custom Tuning")
        tuningComboBox = ComboBox<String>()
        tuningComboBox.items.addAll(tunings.toTypedArray())
        tuningComboBox.selectionModel.select(0)
        tuningComboBox.setOnAction {
            if (tuningComboBox.selectionModel.selectedIndex == Tuning.DEFAULT_TUNINGS.size) {
//                tuningComboBox.transferFocus() TODO
                TuningMakerDialog(this@NewRecordingDialog, customTuning)
            }
        }
        tuningComboBox.setFocusMnemonic("T", scene)

        val loadButton = Button("Load File")
        loadButton.setFocusMnemonic("L", scene)
        loadButton.setOnAction {

            //            val fileChooser = FileChooser()
//            fileChooser.fileFilter = FileNameExtensionFilter("WAV(16 bit PMC)", "wav")
//            fileChooser.currentDirectory = File("res/smallEd.wav")
//            val returnVal = fileChooser.showOpenDialog(parent)
//            if (returnVal == JFileChooser.APPROVE_OPTION) {
//
//                val name = if (nameField.text.isEmpty()) "Nameless" else nameField.text
//                val regex = "$name(\\d| )*".toRegex()
//                val sameNames = recordings.map { it.metaData.name }
//                        .filter { regex.matches(it) }
//                        .map { if (it.length == name.length) 0 else it.substring(name.length).trim().toInt() }
//                        .max()
//                val newName = name + if (sameNames == null) "" else " ${sameNames + 1}"
//
//                val tuning = if (tuningComboBox.selectedIndex == Tuning.DEFAULT_TUNINGS.size)
//                    customTuning ?: Tuning.DEFAULT_TUNINGS[0] // this null case shouldn't happen
//                else
//                    Tuning.DEFAULT_TUNINGS[tuningComboBox.selectedIndex]
//
//                val recording = Recording(tuning, newName)
//                val reader = SoundFileReader(recording, fileChooser.selectedFile)
//
//                try {
//
//                    reader.open()
////                    LoadingDialog("Reading ${fileChooser.selectedFile.name}", "Reading from file", {
////
////                        reader.start()
////                        reader.join()
////
////                    })
//
//                    val session = Session(recording)
//                    session.stepCursor = null
//                    session.onEdited()
////                    MainApplication.push(RecordingEditPane(session))
//                    dispose()
//
//                } catch (e: Exception) {
//                    JOptionPane.showMessageDialog(this@NewRecordingDialog,
//                            "Loading ${fileChooser.selectedFile.name} failed\n${
//                            when (e) {
//                                is javax.sound.sampled.UnsupportedAudioFileException -> "This file format isn't supported"
//                                is SoundFileReader.UnsupportedBitDepthException -> "Only 16 bit depth supported"
//                                is SoundFileReader.UnsupportedChannelsException -> "Only mono supported"
//                                is IOException -> "Read error occurred"
//                                else -> "Unknown error occurred"
//                            }
//                            }", "Error", JOptionPane.ERROR_MESSAGE)
//                }
//
//            }

        }

        val recordButton = Button("Record")
        recordButton.setFocusMnemonic("R", scene)
        recordButton.setOnAction {
            record(nameField, recordings)
        }
        nameField.setOnAction {
            record(nameField, recordings)
        }

        val nameLabel = Label("Name:")
        nameLabel.labelFor = nameField

        val tuningLabel = Label("Tuning:")
        tuningLabel.labelFor = tuningComboBox

        val grid = GridPane()

//        (0 until 2).forEach {
        grid.columnConstraints.add(ColumnConstraints())
        val cc = ColumnConstraints()
        cc.hgrow = Priority.ALWAYS
        GridPane.setHgrow(nameField, Priority.ALWAYS)
        GridPane.setHgrow(tuningComboBox, Priority.ALWAYS)
        grid.columnConstraints.add(cc)
//        }

        nameLabel.alignment = Pos.CENTER_RIGHT
        tuningLabel.alignment = Pos.CENTER_RIGHT

        grid.add(nameLabel, 0, 0)
        grid.add(nameField, 1, 0)
        grid.add(tuningLabel, 0, 1)
        grid.add(tuningComboBox, 1, 1)
        grid.hgap = 5.0
        grid.vgap = 5.0

        val buttons = HBox(5.0)
        buttons.children.addAll(loadButton, recordButton)
        buttons.padding = makeInsets(top = 5.0)
        buttons.alignment = Pos.CENTER
        HBox.setHgrow(loadButton, Priority.ALWAYS)
        HBox.setHgrow(recordButton, Priority.ALWAYS)

        root.center = grid
        root.bottom = buttons
        root.padding = Insets(10.0)

        stage.scene = scene
        stage.showAndWait()

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
        //            MainApplication.push(RecordingEditPane(session))
        stage.close()
    }

    fun refresh(tuning: Tuning?) {

        customTuning = tuning

        if (tuning == null) {

            tuningComboBox.selectionModel.clearSelection()
            tuningComboBox.selectionModel.select(0)

        } else {

            tuningComboBox.items.removeAt(tuningComboBox.items.size - 1)
            tuningComboBox.items.add(tuning.name)
            tuningComboBox.selectionModel.clearSelection()
            tuningComboBox.selectionModel.select(tuningComboBox.items.size - 1)

        }
//        setLocationRelativeTo(MainApplication)

    }

}
