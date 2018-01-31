package dialogs

import components.RecordingEditPane
import core.*
import java.awt.*
import java.io.File
import javax.swing.*
import kotlin.concurrent.thread


class NewRecordingDialog(recordings: MutableList<Recording.PossibleRecording>)
    : JDialog(AppInstance, "New Recording", ModalityType.APPLICATION_MODAL) {

    private var customTuning: Tuning? = null
    private var tuningComboBox: JComboBox<String>

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
        loadButton.addActionListener {
            //            val chooser = JFileChooser()
//            chooser.fileFilter = FileNameExtensionFilter("WAV & MP3", "wav", "mp3")
//            val returnVal = chooser.showOpenDialog(parent)
//            if (returnVal == JFileChooser.APPROVE_OPTION) {
//                println("You chose" + chooser.selectedFile)
//            }

            val name = if (nameField.text.isEmpty()) "Nameless" else nameField.text
            val regex = "$name(\\d| )*".toRegex()
            val sameNames = recordings.map { it.metaData.name }
                    .filter { regex.matches(it) }
                    .map { if (it.length == name.length) 0 else it.substring(name.length).trim().toInt() }
                    .max()
            val newName = name + if (sameNames == null) "" else " ${sameNames + 1}"

            val tuning = if (tuningComboBox.selectedIndex == Tuning.defaultTunings.size)
                customTuning ?: Tuning.defaultTunings[0] // this null case shouldn't happen
            else
                Tuning.defaultTunings[tuningComboBox.selectedIndex]

            val file = File("res/smallEd.wav")
            val recording = Recording(tuning, newName)

            val reader = SoundFileReader(recording, file)

            try {

                reader.open()
                LoadingDialog(this@NewRecordingDialog, file.name) {

                    reader.start()
                    reader.join()

                }

            } catch (e: Exception) { // TODO("different messages")
                println("failed to open stream ${e.message}")
            }

            val session = Session(recording)
            AppInstance.push(RecordingEditPane(session))
            repaint()
            dispose()

        }

        val recordButton = JButton("Record")
        recordButton.setMnemonic('R')
        recordButton.addActionListener {

            val name = if (nameField.text.isEmpty()) "Nameless" else nameField.text
            val regex = "$name(\\d| )*".toRegex()
            val sameNames = recordings.map { it.metaData.name }
                    .filter { regex.matches(it) }
                    .map { if (it.length == name.length) 0 else it.substring(name.length).trim().toInt() }
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
        nameField.addActionListener {
            recordButton.doClick()
        }

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

            tuningComboBox.selectedIndex = 0

        } else {

            tuningComboBox.removeItemAt(tuningComboBox.itemCount - 1)
            tuningComboBox.addItem(tuning.name)
            tuningComboBox.selectedIndex = tuningComboBox.itemCount - 1

        }
        pack()
        setLocationRelativeTo(AppInstance)

    }

    private class LoadingDialog(owner: Window, fileName: String, action: () -> Unit) : JDialog(owner, "Reading from file", ModalityType.APPLICATION_MODAL) {

        init {

            val content = JPanel(BorderLayout())
            content.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

            val label = JLabel("Reading $fileName")
            label.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

            val progressBar = JProgressBar()
            progressBar.isIndeterminate = true

            content.add(label, BorderLayout.NORTH)
            content.add(progressBar, BorderLayout.CENTER)

            contentPane = content
            pack()
            setLocationRelativeTo(owner)
            defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

            thread(name = "Loading Worker") {
                action.invoke()
                dispose()
            }
            isVisible = true

        }

    }


}
