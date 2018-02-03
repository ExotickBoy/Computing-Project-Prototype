package components

import core.AppInstance
import core.AppInstance.ApplicationPane
import core.Recording
import core.Session
import dialogs.LoadingDialog
import dialogs.NewRecordingDialog
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
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
    private lateinit var dataModel: DefaultListModel<Recording.PossibleRecording>

    override fun onCreate() {

        val newButton = JButton("New Recording")
        newButton.setMnemonic('N')

        val editButton = JButton("Edit")
        editButton.setMnemonic('E')
        editButton.isEnabled = false

        val deleteButton = JButton("Delete")
        deleteButton.setMnemonic('D')
        deleteButton.isEnabled = false

        dataModel = DefaultListModel()
        recordingList = JList(dataModel)
        recordingList.setCellRenderer { _, value, _, isSelected, _ ->
            ListElement(value, isSelected)
        }
        recordingList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2)  // double click
                    edit()
            }

        })
        recordingList.addListSelectionListener {
            deleteButton.isEnabled = !recordingList.selectedIndices.isEmpty()
            editButton.isEnabled = recordingList.selectedIndices.size == 1
        }
        recordingList.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER)
                    edit()
                else if (e.keyCode == KeyEvent.VK_DELETE)
                    delete()

            }
        })
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
                    listOf("Delete", "No").toTypedArray(),
                    0)

            if (choice == 0)  // delete pressed
                delete()

        }
        editButton.addActionListener {
            edit()
        }
        val recentRecordingLabel = JLabel("Recent Recordings:")
        recentRecordingLabel.setDisplayedMnemonic('R')
        recentRecordingLabel.labelFor = recordingList

        val buttonPanel = JPanel()

        buttonPanel.add(newButton)
        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
        val recentRecordingPanel = JPanel(BorderLayout())

        recentRecordingPanel.add(recentRecordingLabel, BorderLayout.NORTH)
        recentRecordingPanel.add(JScrollPane(recordingList), BorderLayout.CENTER)

        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        add(recentRecordingPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.NORTH)

    }

    private fun delete() {
        recordingList.selectedIndices.sortedDescending().forEach {
            recordings[it].file.delete()
            recordings.removeAt(it)
            dataModel.removeElementAt(it)
        }
    }

    private fun edit() {
        if (recordingList.selectedIndex != -1) {

            LoadingDialog(AppInstance, "Loading from file", "Loading") {
                val possibleRecording = recordings[recordingList.selectedIndex]
                val session = Session(Recording.deserialize(FileInputStream(possibleRecording.file)))
                AppInstance.push(RecordingEditPane(session))
            }

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
        dataModel.clear()
        recordings.forEach { dataModel.addElement(it) }

    }

    override fun onDestroy() {
    }

    override fun onClose() {
        AppInstance.popAll()
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

}
