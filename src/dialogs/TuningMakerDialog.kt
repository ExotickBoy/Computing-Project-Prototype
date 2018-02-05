package dialogs

import core.Model
import core.Note.Companion.noteStringShort
import core.Note.Companion.pitch
import core.Tuning
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.*

class TuningMakerDialog(private val previous: NewRecordingDialog, tuning: Tuning?)
    : JDialog(null, "Tuning Editor", ModalityType.APPLICATION_MODAL), WindowListener {

    private val strings = mutableListOf<Int>()

    private val nameField: JTextField
    private val capoSpinner: JSpinner
    private val maxFretSpinner: JSpinner

    private val stringsDataModel: DefaultListModel<String>
    private val stringList: JList<String>
    private val newNoteField: JTextField

    private val addButton: JButton
    private val removeButton: JButton
    private val upButton: JButton
    private val downButton: JButton
    private val createButton: JButton

    private fun addString() {

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

    }

    private fun swapIndices(a: Int, b: Int) {

        val temp = strings[a]
        strings[a] = strings[b]
        strings[b] = temp

        stringsDataModel[a] = stringsDataModel[b]
        stringsDataModel[b] = temp.noteStringShort

    }

    init {
        val constraint = GridBagConstraints()

        val capoSpinnerModel = SpinnerNumberModel(tuning?.capo ?: Tuning.DEFAULT_CAPO, 0, Tuning.MAX_MAX_FRET, 1)
        val maxFretSpinnerModel = SpinnerNumberModel(tuning?.maxFret
                ?: Tuning.DEFAULT_MAX_FRET, 0, Tuning.MAX_MAX_FRET, 1)

        nameField = JTextField()
        capoSpinner = JSpinner(capoSpinnerModel)
        maxFretSpinner = JSpinner(maxFretSpinnerModel)

        stringsDataModel = DefaultListModel()
        tuning?.strings?.forEach {
            stringsDataModel.addElement(it.noteStringShort)
        }
        stringList = JList<String>(stringsDataModel)

        val stringsLabel = JLabel("Strings")
        val newNoteLabel = JLabel("To Add:")
        val capoLabel = JLabel("Capo:")
        val maxFretLabel = JLabel("Max Fret:")
        val nameLabel = JLabel("Name:")

        addButton = JButton("Add")
        removeButton = JButton("Remove")
        createButton = JButton("Create")

        upButton = JButton("Up")
        upButton.isEnabled = false
        downButton = JButton("Down")
        downButton.isEnabled = false

        layout = GridBagLayout()


        nameField.addActionListener { nameField.transferFocus() }
        nameLabel.setDisplayedMnemonic('N')
        nameLabel.labelFor = nameField

        capoSpinner.addChangeListener {
            maxFretSpinnerModel.minimum = capoSpinnerModel.number as Int + 1
        }
        capoLabel.setDisplayedMnemonic('p')
        capoLabel.labelFor = capoSpinner

        maxFretSpinner.addChangeListener {
            capoSpinnerModel.maximum = maxFretSpinnerModel.number as Int - 1
        }
        maxFretLabel.setDisplayedMnemonic('M')
        maxFretLabel.labelFor = maxFretSpinner

        stringsLabel.labelFor = stringList
        stringsLabel.setDisplayedMnemonic('S')
        stringList.addListSelectionListener {

            upButton.isEnabled = stringList.selectedIndices.min() != 0
            downButton.isEnabled = stringList.selectedIndices.max() != strings.size - 1

        }

        newNoteField = JTextField()
        newNoteField.addActionListener { addString() }
        newNoteLabel.setDisplayedMnemonic('T')
        newNoteLabel.labelFor = newNoteField

        addButton.setMnemonic('A')
        addButton.addActionListener { addString() }

        removeButton.setMnemonic('R')
        removeButton.addActionListener {
            stringList.selectedIndices.reversed().forEach {
                stringsDataModel.removeElementAt(it)
                strings.removeAt(it)
            }
            stringList.grabFocus()
            repaint()
        }

        upButton.setMnemonic('U')
        upButton.addActionListener {
            if (stringList.selectedIndices.min() ?: 0 != 0) {
                stringList.selectedIndices.sorted().forEach {
                    swapIndices(it - 1, it)
                }
            }
            stringList.selectedIndices = stringList.selectedIndices.map { it - 1 }.toIntArray()
            stringList.grabFocus()

        }

        downButton.setMnemonic('D')
        downButton.addActionListener {
            if (stringList.selectedIndices.max() ?: (strings.size - 1) != strings.size - 1) {
                stringList.selectedIndices.sortedDescending().forEach {
                    swapIndices(it, it + 1)
                }
            }
            stringList.selectedIndices = stringList.selectedIndices.map { it + 1 }.toIntArray()
            stringList.grabFocus()
        }

        createButton.setMnemonic('C')
        createButton.addActionListener {
            val newTuning = Tuning(if (nameField.text.isEmpty()) Tuning.DEFAULT_NAME else nameField.text,
                    strings,
                    capoSpinner.value as Int,
                    maxFretSpinner.value as Int)

            previous.refresh(if (strings.isEmpty()) null else newTuning)
            dispose()
        }

        // LAYOUT

        val topPanel = JPanel(GridBagLayout())

        constraint.weightx = 0.0
        constraint.anchor = GridBagConstraints.EAST
        constraint.fill = GridBagConstraints.NONE
        constraint.gridx = 0
        constraint.gridy = 0
        constraint.insets = Insets(0, 0, INTERNAL_SPACING, INTERNAL_SPACING)
        topPanel.add(nameLabel, constraint)

        constraint.weightx = 1.0
        constraint.anchor = GridBagConstraints.CENTER
        constraint.fill = GridBagConstraints.HORIZONTAL
        constraint.gridx = 1
        constraint.gridy = 0
        constraint.insets = Insets(0, INTERNAL_SPACING, INTERNAL_SPACING, 0)
        topPanel.add(nameField, constraint)

        constraint.weightx = 0.0
        constraint.anchor = GridBagConstraints.EAST
        constraint.fill = GridBagConstraints.NONE
        constraint.gridx = 0
        constraint.gridy = 1
        constraint.insets = Insets(INTERNAL_SPACING, 0, INTERNAL_SPACING, INTERNAL_SPACING)
        topPanel.add(capoLabel, constraint)

        constraint.weightx = 1.0
        constraint.anchor = GridBagConstraints.CENTER
        constraint.fill = GridBagConstraints.HORIZONTAL
        constraint.gridx = 1
        constraint.gridy = 1
        constraint.insets = Insets(0, INTERNAL_SPACING, INTERNAL_SPACING, 0)
        topPanel.add(capoSpinner, constraint)

        constraint.weightx = 0.0
        constraint.anchor = GridBagConstraints.EAST
        constraint.fill = GridBagConstraints.NONE
        constraint.gridx = 0
        constraint.gridy = 2
        constraint.gridwidth = 1
        constraint.insets = Insets(INTERNAL_SPACING, 0, 0, INTERNAL_SPACING)
        topPanel.add(maxFretLabel, constraint)

        constraint.weightx = 1.0
        constraint.anchor = GridBagConstraints.CENTER
        constraint.fill = GridBagConstraints.HORIZONTAL
        constraint.gridx = 1
        constraint.gridy = 2
        constraint.gridwidth = 2
        constraint.insets = Insets(INTERNAL_SPACING, INTERNAL_SPACING, 0, 0)
        topPanel.add(maxFretSpinner, constraint)

        val middlePanel = JPanel(GridBagLayout())

        constraint.gridx = 0
        constraint.gridy = 0
        constraint.anchor = GridBagConstraints.WEST
        constraint.fill = GridBagConstraints.HORIZONTAL
        middlePanel.add(stringsLabel, constraint)

        constraint.gridx = 0
        constraint.gridy = 1
        constraint.anchor = GridBagConstraints.CENTER
        constraint.fill = GridBagConstraints.HORIZONTAL
        middlePanel.add(JScrollPane(stringList), constraint)

        val bottomPanel = JPanel(GridBagLayout())

        constraint.weightx = 1.0
        constraint.fill = GridBagConstraints.NONE
        constraint.anchor = GridBagConstraints.EAST
        constraint.gridx = 0
        constraint.gridy = 0
        constraint.gridwidth = 1

        constraint.insets = Insets(0, 0, INTERNAL_SPACING, INTERNAL_SPACING)
        bottomPanel.add(newNoteLabel, constraint)

        constraint.gridx = 1
        constraint.gridy = 0
        constraint.fill = GridBagConstraints.HORIZONTAL
        constraint.insets = Insets(0, INTERNAL_SPACING, INTERNAL_SPACING, INTERNAL_SPACING)
        bottomPanel.add(newNoteField, constraint)

        constraint.gridx = 2
        constraint.gridy = 0
        constraint.insets = Insets(0, INTERNAL_SPACING, INTERNAL_SPACING, 0)
        bottomPanel.add(addButton, constraint)

        constraint.gridx = 0
        constraint.gridy = 1
        constraint.insets = Insets(INTERNAL_SPACING, 0, INTERNAL_SPACING, INTERNAL_SPACING)
        bottomPanel.add(removeButton, constraint)

        constraint.gridx = 1
        constraint.gridy = 1
        constraint.insets = Insets(INTERNAL_SPACING, INTERNAL_SPACING, INTERNAL_SPACING, INTERNAL_SPACING)
        bottomPanel.add(upButton, constraint)

        constraint.gridx = 2
        constraint.gridy = 1
        constraint.insets = Insets(INTERNAL_SPACING, INTERNAL_SPACING, INTERNAL_SPACING, 0)
        bottomPanel.add(downButton, constraint)

        constraint.gridx = 0
        constraint.gridy = 2
        constraint.gridwidth = 4
        constraint.insets = Insets(INTERNAL_SPACING, 0, 0, 0)
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
        add(middlePanel, constraint)

        constraint.gridx = 0
        constraint.gridy = 2
        constraint.anchor = GridBagConstraints.CENTER
        constraint.fill = GridBagConstraints.HORIZONTAL
        constraint.insets = Insets(10, 10, 10, 10)
        add(bottomPanel, constraint)

        pack()
//        setLocationRelativeTo(previous)
        defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
        isVisible = true

    }

    override fun windowClosing(e: WindowEvent?) {
        previous.refresh(null)
        dispose()
    }

    companion object {

        const val INTERNAL_SPACING = 2

    }


    override fun windowDeiconified(e: WindowEvent?) {}

    override fun windowClosed(e: WindowEvent?) {}

    override fun windowActivated(e: WindowEvent?) {}

    override fun windowDeactivated(e: WindowEvent?) {}

    override fun windowOpened(e: WindowEvent?) {}

    override fun windowIconified(e: WindowEvent?) {}

}
