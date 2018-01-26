package components

import core.*
import core.AppInstance.ApplicationPane
import core.Note.Companion.noteStringShort
import core.Note.Companion.pitch
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.io.File
import java.io.FileInputStream
import javax.swing.*
import kotlin.math.roundToInt


class RecordingsListPane : ApplicationPane() {

    private val recordings: MutableList<Recording.PossibleRecording> = mutableListOf()
    private lateinit var recordingList: JList<Recording.PossibleRecording>
    private val repaintThread = RepaintThread(this)
    private lateinit var dataModel: DefaultListModel<Recording.PossibleRecording>

    override fun onCreate() {

        val buttonPanel = JPanel()
        val newButton = JButton("New Recording")
        newButton.setMnemonic('N')
        val editButton = JButton("Edit")
        editButton.setMnemonic('E')
        val deleteButton = JButton("Delete")
        deleteButton.setMnemonic('D')
        editButton.isEnabled = false
        deleteButton.isEnabled = false

        buttonPanel.add(newButton)
        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

        val recentRecordingPanel = JPanel(BorderLayout())

        dataModel = DefaultListModel()
        recordingList = JList(dataModel)
        recordingList.setCellRenderer { _, value, _, isSelected, _ ->
            ListElement(value, isSelected)
        }
        recordingList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) { // double click
                    val possibleRecording = recordings[recordingList.selectedIndex]
                    val session = Session(Recording.deserialize(FileInputStream(possibleRecording.file)))
                    AppInstance.push(RecordingEditPane(session))
                }
            }
        })
        recordingList.addListSelectionListener {
            deleteButton.isEnabled = !recordingList.selectedIndices.isEmpty()
            editButton.isEnabled = recordingList.selectedIndices.size == 1
        }
        newButton.addActionListener {
            NewRecordingDialog(recordings)
        }
        deleteButton.addActionListener {
            val choice = JOptionPane.showOptionDialog(AppInstance,
                    "Are you sure you want to delete recording${if (recordingList.selectedIndices.size == 1) "" else "s"} \n${recordingList.selectedIndices.map {
                        recordings[it].metaData.name
                    }.reduce { acc, s -> "$acc, ${if (acc.length - acc.lastIndexOf("\n") >= 30) "\n" else ""}$s" }}?",
                    "Delete Confirmation",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    listOf("No", "Delete").toTypedArray(),
                    0)

            if (choice == 1) { // delete pressed

                recordingList.selectedIndices.sortedDescending().forEach {
                    recordings[it].file.delete()
                    recordings.removeAt(it)
                    dataModel.removeElementAt(it)
                }

            }
        }
        editButton.addActionListener {
            val possibleRecording = recordings[recordingList.selectedIndex]
            val session = Session(Recording.deserialize(FileInputStream(possibleRecording.file)))
            AppInstance.push(RecordingEditPane(session))
        }

        recentRecordingPanel.add(JLabel("Recent Recordings:"), BorderLayout.NORTH)
        recentRecordingPanel.add(JScrollPane(recordingList), BorderLayout.CENTER)

        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(recentRecordingPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.NORTH)

    }

    override fun onPause() {
        repaintThread.isPaused = true
    }

    override fun onResume() {
        repaintThread.isPaused = false

        recordings.clear()
        recordings.addAll(Recording.findPossibleRecordings(File(Recording.DEFAULT_PATH)))
        recordings.sortByDescending { it.metaData.lastEdited }
        dataModel.clear()
        recordings.forEach { dataModel.addElement(it) }

    }

    override fun onDestroy() {
    }

    private class ListElement(possibleRecording: Recording.PossibleRecording, selected: Boolean) : JPanel() {

        init {

            layout = BorderLayout()
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            val leftLabel = JLabel(possibleRecording.metaData.name)
            leftLabel.font = leftLabel.font.deriveFont(20f)

            val rightPanel = JPanel(BorderLayout())
            rightPanel.add(JLabel(possibleRecording.metaData.lastEdited.toRelativeTime()), BorderLayout.NORTH)
            rightPanel.add(JLabel(possibleRecording.metaData.length.toLength()), BorderLayout.SOUTH)

            add(leftLabel, BorderLayout.WEST)
            add(rightPanel, BorderLayout.EAST)
            rightPanel.border = BorderFactory.createEmptyBorder(0, 10, 0, 0)

            background = if (selected) SELECTED_COLOUR else background
            rightPanel.background = background

        }

        companion object {

            val SELECTED_COLOUR = Color(186, 212, 255)

        }

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
                    pane.repaint()
                    sleep(100)
                }
            }
        }
    }

    private class NewRecordingDialog(recordings: MutableList<Recording.PossibleRecording>)
        : JDialog(AppInstance, "New Recording", ModalityType.APPLICATION_MODAL) {

        var customTuning: Tuning? = null
        var tuningComboBox: JComboBox<String>

        init {
            layout = GridBagLayout()

            val constraint = GridBagConstraints()

            val nameField = JTextField()
            val tunings = Tuning.defaultTunings.map { it.name }.toMutableList()
            tunings.add("Custom Tuning")
            tuningComboBox = JComboBox(tunings.toTypedArray())
            tuningComboBox.addActionListener {
                if (tuningComboBox.selectedIndex == Tuning.defaultTunings.size) {
                    tuningComboBox.transferFocus()
                    TuningMakerDialog(this@NewRecordingDialog, customTuning)
                }
            }
            val loadButton = JButton("Load File")
            loadButton.setMnemonic('L')
            loadButton.isEnabled = false
            val recordButton = JButton("Record")
            recordButton.setMnemonic('R')

            val nameLabel = JLabel("Name:")
            nameLabel.setDisplayedMnemonic('N')
            nameLabel.labelFor = nameField
            val tuningLabel = JLabel("Tuning:")
            tuningLabel.setDisplayedMnemonic('T')
            tuningLabel.labelFor = tuningComboBox

            constraint.anchor = GridBagConstraints.EAST
            constraint.fill = GridBagConstraints.NONE
            constraint.gridy = 0
            constraint.gridx = 0
            constraint.insets = Insets(10, 10, 5, 5)
            add(nameLabel, constraint)

            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 1
            constraint.insets = Insets(10, 5, 5, 10)
            add(nameField, constraint)
            nameField.addActionListener {
                recordButton.doClick()
            }

            constraint.anchor = GridBagConstraints.EAST
            constraint.gridy = 1
            constraint.fill = GridBagConstraints.NONE
            constraint.gridx = 0
            constraint.insets = Insets(5, 10, 5, 5)
            add(tuningLabel, constraint)

            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 1
            constraint.insets = Insets(5, 5, 5, 10)
            add(tuningComboBox, constraint)

            val buttons = JPanel()
            buttons.add(loadButton, constraint)
            buttons.add(recordButton, constraint)

            loadButton.addActionListener {}
            recordButton.addActionListener {

                val name = if (nameField.text.isEmpty()) "Nameless" else nameField.text
                val regex = "$name(\\d| )*".toRegex()
                println(recordings)
                val sameNames = recordings.map { it.metaData.name }
                        .filter { regex.matches(it) }
                        .map { if (it.length == name.length) 0 else it.substring(name.length).trim().toInt() }
                        .onEach { println(it) }
                        .max()
                val newName = name + if (sameNames == null) "" else " ${sameNames + 1}"

                val tuning = if (tuningComboBox.selectedIndex == Tuning.defaultTunings.size)
                    customTuning ?: Tuning.defaultTunings[0] // this null case shouldn't happen
                else
                    Tuning.defaultTunings[tuningComboBox.selectedIndex]

                val session = Session(Recording(tuning, newName))
                AppInstance.push(RecordingEditPane(session))
                dispose()

            }

            constraint.gridx = 0
            constraint.gridy = 2
            constraint.gridwidth = 2
            constraint.insets = Insets(0, 5, 5, 5)
            add(buttons, constraint)

            pack()
            setLocationRelativeTo(AppInstance)
            isVisible = true
        }

        fun refresh(tuning: Tuning?) {

            customTuning = tuning

            if (tuning == null) {

                tuningComboBox.selectedIndex = tuningComboBox.itemCount - 2

            } else {

                tuningComboBox.removeItemAt(tuningComboBox.itemCount - 1)
                tuningComboBox.addItem(tuning.name)
                tuningComboBox.selectedIndex = tuningComboBox.itemCount - 1

            }
            pack()
            setLocationRelativeTo(AppInstance)

        }

    }

    private class TuningMakerDialog(val previous: NewRecordingDialog, tuning: Tuning?)
        : JDialog(previous, "Tuning Editor", ModalityType.APPLICATION_MODAL), WindowListener {

        val strings = mutableListOf<Int>()

        val nameField: JTextField
        val capoSpinner: JSpinner
        val maxFretSpinner: JSpinner

        val stringsDataModel: DefaultListModel<String>
        val stringList: JList<String>
        val newNoteField: JTextField

        val addButton: JButton
        val deleteButton: JButton
        val upButton: JButton
        val downButton: JButton
        val createButton: JButton

        fun addString() {

            val field = newNoteField.text
            val fieldAsInt = field.toIntOrNull()
            val fieldAsString = field.pitch

            when {
                fieldAsInt != null -> {
                    if (fieldAsInt in Model.POSSIBLE_PITCHES) {
                        strings.add(fieldAsInt)
                        stringsDataModel.addElement(fieldAsInt.noteStringShort)
                    }
                    newNoteField.text = ""
                }
                fieldAsString != null -> {
                    if (fieldAsString in Model.POSSIBLE_PITCHES) {
                        strings.add(fieldAsString)
                        stringsDataModel.addElement(fieldAsString.noteStringShort)
                    }
                    newNoteField.text = ""
                }
            }

            println(strings)

        }

        init {
            val constraint = GridBagConstraints()

            layout = GridBagLayout()


            nameField = JTextField()

            nameField.addActionListener { nameField.transferFocus() }
            val capoSpinnerModel = SpinnerNumberModel(tuning?.capo ?: Tuning.DEFAULT_CAPO, 0, Tuning.MAX_MAX_FRET, 1)
            val maxFretSpinnerModel = SpinnerNumberModel(tuning?.maxFret
                    ?: Tuning.DEFAULT_MAX_FRET, 0, Tuning.MAX_MAX_FRET, 1)
            capoSpinner = JSpinner(capoSpinnerModel)
            maxFretSpinner = JSpinner(maxFretSpinnerModel)

            capoSpinner.addChangeListener {
                maxFretSpinnerModel.minimum = capoSpinnerModel.number as Int + 1
            }

            maxFretSpinner.addChangeListener {
                capoSpinnerModel.maximum = maxFretSpinnerModel.number as Int - 1
            }

            stringsDataModel = DefaultListModel()
            tuning?.strings?.forEach {
                stringsDataModel.addElement(it.noteStringShort)
            }
            stringList = JList<String>(stringsDataModel)

            newNoteField = JTextField()
            newNoteField.addActionListener { addString() }

            val newNoteLabel = JLabel("To Add:")
            newNoteLabel.setDisplayedMnemonic('T')
            newNoteLabel.labelFor = newNoteField

            val capoLabel = JLabel("Capo:")
            capoLabel.setDisplayedMnemonic('p')
            capoLabel.labelFor = capoSpinner
            val maxFretLabel = JLabel("Max Fret:")
            maxFretLabel.setDisplayedMnemonic('M')
            maxFretLabel.labelFor = maxFretSpinner
            val nameLabel = JLabel("Name:")
            nameLabel.setDisplayedMnemonic('N')
            nameLabel.labelFor = nameField

            addButton = JButton("Add")
            addButton.setMnemonic('A')
            addButton.addActionListener { addString() }
            deleteButton = JButton("Delete")
            deleteButton.setMnemonic('D')
            upButton = JButton("Up")
            upButton.setMnemonic('U')
            downButton = JButton("Down")
            downButton.setMnemonic('o')
            createButton = JButton("Create")
            createButton.setMnemonic('C')
            createButton.addActionListener {
                val newTuning = Tuning(if (nameField.text.isEmpty()) Tuning.DEFAULT_NAME else nameField.text,
                        strings,
                        capoSpinner.value as Int,
                        maxFretSpinner.value as Int)

                println(newTuning)
                previous.refresh(newTuning)
                dispose()
            }

            // LAYOUT

            val topPanel = JPanel(GridBagLayout())

            constraint.weightx = 0.0
            constraint.anchor = GridBagConstraints.EAST
            constraint.fill = GridBagConstraints.NONE
            constraint.gridx = 0
            constraint.gridy = 0
            topPanel.add(nameLabel, constraint)

            constraint.weightx = 1.0
            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 1
            constraint.gridy = 0
            topPanel.add(nameField, constraint)

            constraint.weightx = 0.0
            constraint.anchor = GridBagConstraints.EAST
            constraint.fill = GridBagConstraints.NONE
            constraint.gridx = 0
            constraint.gridy = 1
            topPanel.add(capoLabel, constraint)

            constraint.weightx = 1.0
            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 1
            constraint.gridy = 1
            topPanel.add(capoSpinner, constraint)

            constraint.weightx = 0.0
            constraint.anchor = GridBagConstraints.EAST
            constraint.fill = GridBagConstraints.NONE
            constraint.gridx = 0
            constraint.gridy = 2
            constraint.gridwidth = 1
            topPanel.add(maxFretLabel, constraint)

            constraint.weightx = 1.0
            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 1
            constraint.gridy = 2
            constraint.gridwidth = 2
            topPanel.add(maxFretSpinner, constraint)

            val bottomPanel = JPanel(GridBagLayout())

            constraint.weightx = 1.0
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 0
            constraint.gridy = 0
            constraint.gridwidth = 2
            constraint.fill = GridBagConstraints.HORIZONTAL
            bottomPanel.add(newNoteLabel, constraint)

            constraint.gridx = 1
            constraint.gridy = 0
            constraint.gridwidth = 1
            bottomPanel.add(newNoteField, constraint)

            constraint.gridx = 2
            constraint.gridy = 0
            constraint.gridwidth = 1
            bottomPanel.add(addButton, constraint)

            constraint.gridx = 0
            constraint.gridy = 1
            bottomPanel.add(deleteButton, constraint)

            constraint.gridx = 1
            constraint.gridy = 1
            constraint.gridwidth = 1
            bottomPanel.add(upButton, constraint)

            constraint.gridx = 2
            constraint.gridy = 1
            constraint.gridwidth = 1
            bottomPanel.add(downButton, constraint)

            constraint.gridx = 0
            constraint.gridy = 2
            constraint.gridwidth = 4
            bottomPanel.add(createButton, constraint)

            constraint.gridx = 0
            constraint.gridy = 0
            constraint.gridwidth = 1
            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.insets = Insets(10, 10, 10, 10)
            add(topPanel, constraint)

            constraint.gridx = 0
            constraint.gridy = 1
            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.insets = Insets(0, 10, 0, 10)
            add(JScrollPane(stringList), constraint)

            constraint.gridx = 0
            constraint.gridy = 2
            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.insets = Insets(10, 10, 10, 10)
            add(bottomPanel, constraint)

            pack()
            setLocationRelativeTo(previous)
            defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            isVisible = true

        }

        override fun windowDeiconified(e: WindowEvent?) {}

        override fun windowClosing(e: WindowEvent?) {
            previous.refresh(null)
            dispose()
        }

        override fun windowClosed(e: WindowEvent?) {}

        override fun windowActivated(e: WindowEvent?) {}

        override fun windowDeactivated(e: WindowEvent?) {}

        override fun windowOpened(e: WindowEvent?) {}

        override fun windowIconified(e: WindowEvent?) {}

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
