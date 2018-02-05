package dialogs

import components.RecordingsListPane.Companion.setFocusMnemonic
import core.MainApplication
import core.Model
import core.Note.Companion.noteStringShort
import core.Note.Companion.pitch
import core.Tuning
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment
import javafx.stage.Modality
import javafx.stage.Stage

/**
 * This class encapsulates creates and encapsulates the behaviours of the dialog which is used to create tunings.
 * The dialog consists of : a name field, capo spinner, max fret spinner, string list, new string field, add button,
 * remove button, move up, move down and create button.
 *
 * @author Kacper Lubisz
 *
 * @property previous The NewRecordingDialog that created the tuning, this is for passing the created tuning back
 *
 * @param tuning the tuning to be edited, null if new tuning is to be created
 *
 * @see Tuning
 * @see NewRecordingDialog
 *
 */
class TuningMakerDialog(private val previous: NewRecordingDialog, tuning: Tuning?) {

    // the window
    private val stage: Stage = Stage()

    // GUI elements
    private val strings = mutableListOf<Int>()

    private val nameField: TextField
    private val capoSpinner: Spinner<Int>
    private val maxFretSpinner: Spinner<Int>

    private val stringList: ListView<String>
    private val newStringField: TextField

    private val addButton: Button
    private val removeButton: Button
    private val upButton: Button
    private val downButton: Button
    private val createButton: Button

    /**
     * This function reads the contents of the new string field and then either adds a new string or gives the user
     * feedback that their input is invalid.
     *
     * One valid format for input is the text version of the note, for example G#3.
     * Another valid input is just the number of the corresponding note, since this is something only really
     *      relevant to this software I don't really expect users to use this
     *
     */
    private fun addString() {

        val field = newStringField.text
        val fieldAsInt = field.toIntOrNull()
        val fieldAsString = field.pitch

        when {
            fieldAsInt != null && fieldAsInt in Model.POSSIBLE_PITCHES -> {

                strings.add(fieldAsInt)
                stringList.items.add(fieldAsInt.noteStringShort)

                newStringField.text = ""
                newStringField.style = ""
            }
            fieldAsString != null && fieldAsString in Model.POSSIBLE_PITCHES -> {

                strings.add(fieldAsString)
                stringList.items.add(fieldAsString.noteStringShort)

                newStringField.text = ""
                newStringField.style = ""
            }
            else -> {
                newStringField.style = "-fx-focus-color:red;"
                // this gives the field a red glow when it is selected
            }
        }

    }

    /**
     *This function is simply for swapping the place of two strings
     * @param a the index of the first one to be swapped
     * @param b the index of the second one to be swapped
     */
    private fun swapIndices(a: Int, b: Int) {

        val temp = strings[a]
        strings[a] = strings[b]
        strings[b] = temp

        stringList.items[a] = stringList.items[b]
        stringList.items[b] = temp.noteStringShort

    }

    init {

        // you can't return to the window below the dialog until the dialog is closed
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "New Tuning"
        if (MainApplication.icon != null)
            stage.icons.add(MainApplication.icon)

        val root = GridPane()
        val scene = Scene(root)

        // the constraints to what values the capo spinner can take
        val capoSpinnerValueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(
                0,
                (tuning?.maxFret ?: (Tuning.MAX_MAX_FRET + 1)) - 1,
                tuning?.capo ?: Tuning.DEFAULT_CAPO,
                1
        )
        capoSpinner = Spinner(capoSpinnerValueFactory)
        capoSpinner.maxWidth = Double.MAX_VALUE
        capoSpinner.setFocusMnemonic("A", scene)

        val maxFretSpinnerValueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(
                (tuning?.capo ?: -1) + 1,
                Tuning.MAX_MAX_FRET,
                tuning?.maxFret ?: Tuning.DEFAULT_MAX_FRET,
                1
        )
        maxFretSpinner = Spinner(maxFretSpinnerValueFactory)
        maxFretSpinner.maxWidth = Double.MAX_VALUE
        maxFretSpinner.setFocusMnemonic("M", scene)

        capoSpinner.valueProperty().addListener { _ ->
            maxFretSpinnerValueFactory.min = capoSpinner.value as Int + 1
        }
        maxFretSpinner.valueProperty().addListener { _ ->
            capoSpinnerValueFactory.max = maxFretSpinner.value as Int - 1
        }

        nameField = TextField(tuning?.name ?: "")
        nameField.promptText = "Tuning Name"
        nameField.maxWidth = Double.MAX_VALUE
        nameField.setFocusMnemonic("N", scene)

        newStringField = TextField()
        newStringField.promptText = "New Note (Enter to Add)"
        newStringField.setFocusMnemonic("O", scene)
        newStringField.textProperty().addListener { _ -> newStringField.style = "" }
        newStringField.setOnAction { addString() }

        upButton = Button("Up")
        upButton.maxWidth = Double.MAX_VALUE
        upButton.isDisable = true
        downButton = Button("Down")
        downButton.maxWidth = Double.MAX_VALUE
        downButton.isDisable = true

        val placeholderLabel = Label("Type a new note (e.g. G#3)\n&\nPress 'Add' to add a string")
        placeholderLabel.textAlignment = TextAlignment.CENTER
        stringList = ListView()
        stringList.placeholder = placeholderLabel
        stringList.setFocusMnemonic("S", scene)
        stringList.prefHeight = 150.0
        stringList.selectionModel.selectionMode = SelectionMode.MULTIPLE
        tuning?.strings?.forEach {
            stringList.items.add(it.noteStringShort)
            strings.add(it)
        } // add the strings of the already existing tuning to the list
        stringList.selectionModel.selectedIndexProperty().addListener { _ ->

            upButton.isDisable = stringList.selectionModel.selectedIndices.min() == 0
            downButton.isDisable = stringList.selectionModel.selectedIndices.max() == strings.size - 1

        }

        val stringsLabel = Label("Strings")
        stringsLabel.labelFor = stringList
        val capoLabel = Label("Capo:")
        capoLabel.labelFor = capoSpinner
        val maxFretLabel = Label("Max Fret:")
        maxFretLabel.labelFor = maxFretSpinner
        val nameLabel = Label("Name:")
        nameLabel.labelFor = nameField

        addButton = Button("Add")
        addButton.maxWidth = Double.MAX_VALUE
        addButton.setFocusMnemonic("A", scene)
        addButton.setOnAction { addString() }

        removeButton = Button("Remove")
        removeButton.maxWidth = Double.MAX_VALUE
        removeButton.setFocusMnemonic("R", scene)
        removeButton.setOnAction {
            stringList.selectionModel.selectedIndices.reversed().forEach {
                strings.removeAt(it)
                stringList.items.removeAt(it)
            }
        }

        createButton = Button("Create")
        createButton.maxWidth = Double.MAX_VALUE
        createButton.setFocusMnemonic("C", scene)
        createButton.setOnAction {
            // closes the dialog
            val newTuning = Tuning(if (nameField.text.isEmpty()) Tuning.DEFAULT_NAME else nameField.text,
                    strings,
                    capoSpinner.value as Int,
                    maxFretSpinner.value as Int)

            previous.refresh(if (strings.isEmpty()) null else newTuning)
            stage.close()
        }

        upButton.setFocusMnemonic("U", scene)
        upButton.setOnAction {
            // move all the selected strings up by one
            if (stringList.selectionModel.selectedIndices.min() != 0) {
                val indices = stringList.selectionModel.selectedIndices.sorted().toList()
                indices.forEach {
                    swapIndices(it - 1, it)
                }
                stringList.selectionModel.clearSelection()
                indices.map { it - 1 }.forEach {
                    stringList.selectionModel.select(it)
                }
            }
        }
        downButton.setFocusMnemonic("D", scene)
        downButton.setOnAction {
            if (stringList.selectionModel.selectedIndices.max() != strings.size - 1) {

                val indices = stringList.selectionModel.selectedIndices.sortedDescending().toList()
                indices.forEach {
                    swapIndices(it, it + 1)
                }
                stringList.selectionModel.clearSelection()
                indices.map { it + 1 }.forEach {
                    stringList.selectionModel.select(it)
                }

            }
        }

        // laying out

        val detailPanel = GridPane()
        detailPanel.hgap = 5.0
        detailPanel.vgap = 5.0
        detailPanel.add(nameLabel, 0, 0)
        detailPanel.add(nameField, 1, 0)
        detailPanel.add(capoLabel, 0, 1)
        detailPanel.add(capoSpinner, 1, 1)
        detailPanel.add(maxFretLabel, 0, 2)
        detailPanel.add(maxFretSpinner, 1, 2)
        detailPanel.maxWidth = Double.MAX_VALUE
        detailPanel.alignment = Pos.CENTER

        val listPanel = VBox() // vertical flow
        listPanel.children.addAll(stringsLabel, stringList)
        listPanel.maxWidth = Double.MAX_VALUE

        val buttonPanel = GridPane()
        buttonPanel.hgap = 5.0
        buttonPanel.vgap = 5.0
        buttonPanel.maxWidth = Double.MAX_VALUE
        buttonPanel.alignment = Pos.CENTER
        buttonPanel.add(newStringField, 0, 0, 4, 1)
        buttonPanel.add(addButton, 0, 1)
        buttonPanel.add(removeButton, 1, 1)
        buttonPanel.add(upButton, 2, 1)
        buttonPanel.add(downButton, 3, 1)
        buttonPanel.add(createButton, 0, 2, 4, 1)
        // column index, rowIndex, column span, row span

        root.add(detailPanel, 0, 0)
        root.add(listPanel, 0, 1)
        root.add(buttonPanel, 0, 2)
        root.vgap = 5.0
        root.padding = Insets(10.0)

        stage.scene = scene
        stage.setOnCloseRequest {
            it.consume()
            previous.refresh(null) // pass an empty tuning back
            stage.close()
        }
        stage.showAndWait()
        // this will hold the thread here until the stage is closed

    }

}
