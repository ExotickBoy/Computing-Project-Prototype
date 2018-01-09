package core

import components.ContentsPane
import javax.swing.JFrame

fun main(args: Array<String>) {

    val frame = JFrame("NoteWize™ Prototype")

    val tuning = Tuning("E2", "A2", "D3", "G3", "B3", "E4")
    val recording = Recording(tuning, "Nameless")
    val session = Session(recording)

    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.contentPane = ContentsPane(session)
    frame.pack()
    frame.isVisible = true

}

