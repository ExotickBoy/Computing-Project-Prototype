package components

import core.AppInstance
import core.Session
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

internal class ControlPane(private val session: Session) : JPanel() {

    private val recordButton = JButton(RECORD_EMOJI)
    private val pauseRecordingButton = JButton(STOP_EMOJI)
    private val playbackButton = JButton(PLAY_EMOJI)
    private val pausePlaybackButton = JButton(PAUSE_EMOJI)
    private val exitButton = JButton(EXIT_BUTTON_TEXT)

    private val cutButton = JButton(SCISSORS_EMOJI)

    init {

        session.addOnStateChange {
            when {
                session.isEditSafe -> {

                    recordButton.isVisible = true
                    pauseRecordingButton.isVisible = false
                    playbackButton.isVisible = true
                    pausePlaybackButton.isVisible = false

                    recordButton.isEnabled = true
                    playbackButton.isEnabled = true

                    cutButton.isEnabled = true

                }
                session.isRecording -> {

                    recordButton.isVisible = false
                    pauseRecordingButton.isVisible = true
                    playbackButton.isVisible = true
                    pausePlaybackButton.isVisible = false

                    pauseRecordingButton.isEnabled = true
                    playbackButton.isEnabled = false

                    cutButton.isEnabled = false
                }
                else -> {

                    recordButton.isVisible = true
                    pauseRecordingButton.isVisible = false
                    playbackButton.isVisible = false
                    pausePlaybackButton.isVisible = true

                    recordButton.isEnabled = true
                    pausePlaybackButton.isEnabled = true

                    cutButton.isEnabled = false

                }
            }

        }

        recordButton.addActionListener {
            if (session.record()) {

                pauseRecordingButton.isVisible = true
                recordButton.isVisible = false

                playbackButton.isEnabled = false
                cutButton.isEnabled = false

            } else {
                recordButton.isEnabled = false
            }
        }


        pauseRecordingButton.isVisible = false
        pauseRecordingButton.addActionListener {
            if (session.pauseRecording()) {

                recordButton.isVisible = true
                pauseRecordingButton.isVisible = false

                playbackButton.isEnabled = true
                cutButton.isEnabled = true
            }
        }

        playbackButton.isEnabled = false
        playbackButton.addActionListener {
            if (session.playback()) {
                playbackButton.isVisible = false
                pausePlaybackButton.isVisible = true

                recordButton.isEnabled = false
                cutButton.isEnabled = false

            }
        }

        pausePlaybackButton.isVisible = false
        pausePlaybackButton.addActionListener {
            if (session.pausePlayback()) {
                pausePlaybackButton.isVisible = false
                playbackButton.isVisible = true

                recordButton.isEnabled = true
                cutButton.isEnabled = true
            }
        }

        cutButton.isEnabled = false
        cutButton.addActionListener {
            if (session.isEditSafe) {
                session.makeCut(session.correctedStepCursor)
            }
        }

        exitButton.addActionListener {
            AppInstance.pop()
        }

        val centrePanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0))

        centrePanel.add(recordButton)
        centrePanel.add(pauseRecordingButton)
        centrePanel.add(playbackButton)
        centrePanel.add(pausePlaybackButton)
        centrePanel.add(cutButton)

        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        add(exitButton, BorderLayout.LINE_START)
        add(centrePanel, BorderLayout.CENTER)

    }

    companion object {

        private const val PLAY_EMOJI = "▶"
        private const val RECORD_EMOJI = "\u23FA"
        private const val PAUSE_EMOJI = "❚❚"
        private const val STOP_EMOJI = "\u23F9"
        private const val SCISSORS_EMOJI = "\u2702"
        private const val EXIT_BUTTON_TEXT = "Exit"

    }

}