package core

import components.ContentsPane
import javax.swing.JFrame

fun main(args: Array<String>) {

    val tuning = Tuning("E2", "A2", "D3", "G3", "B3", "E4")
    val recording: Recording = Recording(tuning, "Nameless")

    val frame = JFrame("Fourier Transform Thingy")

    try {

        val analyser = Analyser(recording, frame)

        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.contentPane = ContentsPane(analyser)
        frame.pack()
        frame.isVisible = true

        analyser.start()

    } catch (e: IllegalArgumentException) {

        // TODO show a microphone is unavailable screen
        println("Couldn't open a microphone line")

    }

}

