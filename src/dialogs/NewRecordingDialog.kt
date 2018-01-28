package dialogs

import components.RecordingEditPane
import core.AppInstance
import core.Recording
import core.Session
import core.Tuning
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

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

}
