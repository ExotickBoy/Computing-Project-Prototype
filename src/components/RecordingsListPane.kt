package components

import core.AppInstance
import core.AppInstance.ApplicationPane
import core.Recording
import core.Session
import core.Tuning
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.FileInputStream
import javax.swing.*


class RecordingsListPane : ApplicationPane() {

    private val recordings: MutableList<Recording.PossibleRecording> = mutableListOf()
    private lateinit var recordingList: JList<Recording.PossibleRecording>

    override fun onCreate() {

        recordings.clear()
        recordings.addAll(Recording.findPossibleRecordings(File(Recording.DEFAULT_PATH)))

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
        recordings.sortedByDescending { it.metaData.lastEdited }.forEach { dataModel.addElement(it) }
        recordingList = JList(dataModel)
        recordingList.setCellRenderer { list, value, index, isSelected, cellHasFocus ->
            ListElement(value, isSelected)
        }
        recordingList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) { // double click
                    val index = (event.source as JList<*>).locationToIndex(event.point)

                    println("gonna open $index")
                }
            }
        })
        recordingList.addListSelectionListener {
            deleteButton.isEnabled = !recordingList.selectedIndices.isEmpty()
            editButton.isEnabled = recordingList.selectedIndices.size == 1
        }
        newButton.addActionListener {
            //            val session = Session(Recording());
//            AppInstance.push(RecordingEditPane())
        }
        deleteButton.addActionListener {
            println("gonna delete ${recordingList.selectedIndices.toList()}")
        }
        editButton.addActionListener {
            val possibleRecording = recordings[recordingList.selectedIndex]
            val session = Session(Recording.deserialize(FileInputStream(possibleRecording.file)));
            AppInstance.push(RecordingEditPane(session))
//            println("gonna edit ${recordingList.selectedIndices.toList()}")
        }

        recentRecordingPanel.add(JLabel("Recent Recordings:"), BorderLayout.NORTH)
        recentRecordingPanel.add(JScrollPane(recordingList), BorderLayout.CENTER)

        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(recentRecordingPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.NORTH)

    }

    override fun onPause() {
    }

    override fun onResume() {
    }

    override fun onDestroy() {
    }

    init {
        val tuning = Tuning.defaultTunings[0]
        val recording = Recording(tuning, "Nameless")
        val session = Session(recording)
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

}

private fun Double.toLength(): String {
    return "some length"
}

private fun Long.toRelativeTime(): String {
    return "some time ago"
}
