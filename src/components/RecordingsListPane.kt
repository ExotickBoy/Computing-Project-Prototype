package components

import core.AppInstance
import core.AppInstance.ApplicationPane
import core.Recording
import core.Session
import core.Tuning
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.FileInputStream
import javax.swing.*
import kotlin.math.roundToInt


class RecordingsListPane : ApplicationPane() {

    private val recordings: MutableList<Recording.PossibleRecording> = mutableListOf()
    private lateinit var recordingList: JList<Recording.PossibleRecording>
    private val repaintThread = RepaintThread(this)

    override fun onCreate() {

        recordings.clear()
        recordings.addAll(Recording.findPossibleRecordings(File(Recording.DEFAULT_PATH)))
        recordings.sortByDescending { it.metaData.lastEdited }

        val buttonPanel = JPanel()
        val newButton = JButton("New Recording")
        val editButton = JButton("Edit")
        val deleteButton = JButton("Delete")
        editButton.isEnabled = false
        deleteButton.isEnabled = false

        buttonPanel.add(newButton)
        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

        val recentRecordingPanel = JPanel(BorderLayout())

        val dataModel = DefaultListModel<Recording.PossibleRecording>()
        recordings.forEach { dataModel.addElement(it) }
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
            NewRecordingDialog()
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
            rightPanel.add(JLabel(possibleRecording.metaData.created.toRelativeTime()), BorderLayout.NORTH)
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

    private class NewRecordingDialog : JDialog(AppInstance, "New Recording", ModalityType.APPLICATION_MODAL) {

        init {
            layout = GridBagLayout()

            val constraint = GridBagConstraints()

            val nameField = JTextField()
            val tuningComboBox = JComboBox<Tuning>()

            val loadButton = JButton("Load File")
            val recordButton = JButton("Record")

            constraint.anchor = GridBagConstraints.EAST
            constraint.fill = GridBagConstraints.NONE
            constraint.gridy = 0
            constraint.gridx = 0
            constraint.insets = Insets(10, 10, 5, 5)
            add(JLabel("Name:"), constraint)

            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 1
            constraint.insets = Insets(10, 5, 5, 10)
            add(nameField, constraint)
            nameField.addActionListener {
                recordButton.grabFocus()
            }

            constraint.anchor = GridBagConstraints.EAST
            constraint.gridy = 1
            constraint.fill = GridBagConstraints.NONE
            constraint.gridx = 0
            constraint.insets = Insets(5, 10, 5, 5)
            add(JLabel("Tuning:"), constraint)

            constraint.anchor = GridBagConstraints.CENTER
            constraint.fill = GridBagConstraints.HORIZONTAL
            constraint.gridx = 1
            constraint.insets = Insets(5, 5, 5, 10)
            add(tuningComboBox, constraint)

            val buttons = JPanel()
            buttons.add(loadButton, constraint)
            buttons.add(recordButton, constraint)

            loadButton.addActionListener {

            }
            recordButton.addActionListener {
                //                        val session = Session(Recording(Tuning.defaultTunings[tuningComboBox.selectedIndex], ""))
//                        AppInstance.push(RecordingEditPane(session))
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
        minutes < 60 -> "${minutes.roundToInt()} minutes ago"
        hours < 24 -> "${hours.roundToInt()} hours ago"
        days < 7 -> "${days.roundToInt()} days ago"
        weeks < 52 -> "${weeks.roundToInt()} weeks ago"
        else -> "over a year ago"
    }
}
