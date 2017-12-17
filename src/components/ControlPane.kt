package components

import core.Session
import javax.swing.JButton
import javax.swing.JPanel

class ControlPane(private val session: Session) : JPanel() {

    private val pauseButton = JButton("Pause")
    private val resumeButton = JButton("Resume")
    private val cutButton = JButton("Cut")
    private val startButton = JButton("Start")

    init {

        pauseButton.isVisible = false
        pauseButton.addActionListener {
            session.analyser.pause()
            pauseButton.isVisible = false
            resumeButton.isVisible = true
            cutButton.isVisible = true
        }
        add(pauseButton)

        resumeButton.isVisible = false
        resumeButton.addActionListener {
            session.analyser.resume()
            pauseButton.isVisible = true
            resumeButton.isVisible = false
            cutButton.isVisible = false
        }
        add(resumeButton)

        cutButton.isVisible = false
        cutButton.addActionListener {
            if (session.cursor != -1) {
                session.recording.cut(session.cursor)
            }
        }
        add(cutButton)

        startButton.addActionListener {
            session.analyser.start()
            startButton.isVisible = false
            pauseButton.isVisible = true
        }
        add(startButton)


    }

}