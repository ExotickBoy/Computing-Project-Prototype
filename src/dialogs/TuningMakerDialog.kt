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

        stage.title = DIALOG_TITLE
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

        capoSpinner.setFocusMnemonic(CAPO_SPINNER_MNEMONIC, scene)

        val maxFretSpinnerValueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(
                (tuning?.capo ?: -1) + 1,
                Tuning.MAX_MAX_FRET,
                tuning?.maxFret ?: Tuning.DEFAULT_MAX_FRET,
                1
        )
        maxFretSpinner = Spinner(maxFretSpinnerValueFactory)
        maxFretSpinner.maxWidth = Double.MAX_VALUE

        maxFretSpinner.setFocusMnemonic(MAX_FRET_SPINNER_MNEMONIC, scene)

        capoSpinner.valueProperty().addListener { _ ->
            maxFretSpinnerValueFactory.min = capoSpinner.value as Int + 1
        }
        maxFretSpinner.valueProperty().addListener { _ ->
            capoSpinnerValueFactory.max = maxFretSpinner.value as Int - 1
        }

        nameField = TextField(tuning?.name ?: "")
        nameField.promptText = NO_TUNING_PROMPT_TEXT
        nameField.maxWidth = Double.MAX_VALUE
        nameField.setFocusMnemonic(NEW_FIELD_MNEMONIC, scene)

        newStringField = TextField()
        newStringField.promptText = NEW_STRING_FIELD_PROMPT

        newStringField.setFocusMnemonic(NEW_STRING_FIELD_MNEMONIC, scene)
        newStringField.textProperty().addListener { _ -> newStringField.style = "" }
        newStringField.setOnAction { addString() }

        upButton = Button(UP_BUTTON_TEXT)
        upButton.maxWidth = Double.MAX_VALUE
        upButton.isDisable = true

        downButton = Button(DOWN_BUTTON_TEXT)
        downButton.maxWidth = Double.MAX_VALUE
        downButton.isDisable = true

        val placeholderLabel = Label(NO_STRING_PLACEHOLDER)
        placeholderLabel.textAlignment = TextAlignment.CENTER
        stringList = ListView()
        stringList.placeholder = placeholderLabel

        stringList.setFocusMnemonic(STRING_LIST_MNEMONIC, scene)
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

        val stringsLabel = Label(STRING_LABEL_TEXT)
        stringsLabel.labelFor = stringList
        val capoLabel = Label(CAPO_LABEL_TEXT)
        capoLabel.labelFor = capoSpinner
        val maxFretLabel = Label(MAX_FRET_LABEL_TEXT)
        maxFretLabel.labelFor = maxFretSpinner
        val nameLabel = Label(NAME_LABEL_TEXT)
        nameLabel.labelFor = nameField

        addButton = Button(ADD_BUTTON_LABEL)
        addButton.maxWidth = Double.MAX_VALUE
        addButton.setFocusMnemonic(ADD_BUTTON_MENMONIC, scene)
        addButton.setOnAction { addString() }

        removeButton = Button(REMOVE_BUTTON_TEXT)
        removeButton.maxWidth = Double.MAX_VALUE
        removeButton.setFocusMnemonic(REMOVE_BUTTON_MNEMONIC, scene)
        removeButton.setOnAction {
            stringList.selectionModel.selectedIndices.reversed().forEach {
                strings.removeAt(it)
                stringList.items.removeAt(it)
            }
        }

        createButton = Button(CREATE_BUTTON_TEXT)
        createButton.maxWidth = Double.MAX_VALUE
        createButton.setFocusMnemonic(CREATE_BUTTON_MNEMONIC, scene)
        createButton.setOnAction {
            // closes the dialog
            val newTuning = Tuning(if (nameField.text.isEmpty()) Tuning.DEFAULT_NAME else nameField.text,
                    strings,
                    capoSpinner.value as Int,
                    maxFretSpinner.value as Int)

            previous.refresh(if (strings.isEmpty()) null else newTuning)
            stage.close()
        }

        upButton.setFocusMnemonic(UP_BUTTON_MNEMONIC, scene)
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
        downButton.setFocusMnemonic(DOWN_BUTTON_MNEMONIC, scene)
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

        detailPanel.hgap = BUTTON_SPACINGS
        detailPanel.vgap = BUTTON_SPACINGS
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
        buttonPanel.hgap = BUTTON_SPACINGS
        buttonPanel.vgap = BUTTON_SPACINGS
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

    companion object {

        private const val STRING_LABEL_TEXT = "Strings"

        private const val CAPO_LABEL_TEXT = "Capo:"
        private const val MAX_FRET_LABEL_TEXT = "Max Fret:"
        private const val NAME_LABEL_TEXT = "Name:"
        private const val REMOVE_BUTTON_TEXT = "Remove"
        private const val ADD_BUTTON_LABEL = "Add"
        private const val CREATE_BUTTON_TEXT = "Create"
        private const val UP_BUTTON_TEXT = "Up"
        private const val DOWN_BUTTON_TEXT = "Down"
        private const val NEW_STRING_FIELD_PROMPT = "New Note (Enter to Add)"
        private const val NO_TUNING_PROMPT_TEXT = "Tuning Name"
        private const val ADD_BUTTON_MENMONIC = "A"

        private const val UP_BUTTON_MNEMONIC = "U"
        private const val DOWN_BUTTON_MNEMONIC = "D"
        private const val NEW_STRING_FIELD_MNEMONIC = "O"
        private const val REMOVE_BUTTON_MNEMONIC = "R"
        private const val CREATE_BUTTON_MNEMONIC = "C"
        private const val CAPO_SPINNER_MNEMONIC = "A"
        private const val MAX_FRET_SPINNER_MNEMONIC = "M"
        private const val NEW_FIELD_MNEMONIC = "N"
        private const val STRING_LIST_MNEMONIC = "S"

        private const val NO_STRING_PLACEHOLDER = "Type a new note (e.g. G#3)\n&\nPress '$ADD_BUTTON_LABEL' to add a string"
        private const val DIALOG_TITLE = "New Tuning"

        const val BUTTON_SPACINGS = 5.0

    }

}
