package components

import core.Analyser
import javax.swing.JButton
import javax.swing.JPanel

class ControlPane(val analyser: Analyser) : JPanel() {

    init {

        val pauseButton = JButton("Pause")
        pauseButton.isVisible = false

        val cutButton = JButton("Cut")
        cutButton.isVisible = false

        val startButton = JButton("Start")
        startButton.addActionListener {

            if (analyser.isRunning) {

                analyser.stop()
                startButton.isVisible = false
                pauseButton.isVisible = false
                cutButton.isVisible = true

            } else {

                try {

                    analyser.start()
                    startButton.text = "Stop"
                    pauseButton.isVisible = true

                } catch (e: IllegalArgumentException) {

                    // TODO show a microphone is unavailable screen
                    println("Couldn't open a microphone line")

                }


            }

        }

        add(pauseButton)
        add(cutButton)
        add(startButton)

    }

}