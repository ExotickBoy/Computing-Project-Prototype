package core

import components.ContentsPane
import javax.swing.JFrame

fun main(args: Array<String>) {

    val frame = JFrame("NoteWize™ Prototype")

    val tuning = Tuning.defaultTunings[0]
    val recording = Recording(tuning, "Nameless")
    val session = Session(recording)

    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane = ContentsPane(session)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true

}

