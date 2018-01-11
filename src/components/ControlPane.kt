package components

import core.Session
import javax.swing.JButton
import javax.swing.JPanel

internal class ControlPane(private val session: Session) : JPanel() {

    private val recordButton = JButton(RECORD_EMOJI)
    private val pauseRecordingButton = JButton(STOP_EMOJI)
    private val playbackButton = JButton(PLAY_EMOJI)
    private val pausePlaybackButton = JButton(PAUSE_EMOJI)

    private val cutButton = JButton(SCISSORS_EMOJI)

    init {

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
        add(recordButton)

        pauseRecordingButton.isVisible = false
        pauseRecordingButton.addActionListener {
            if (session.pauseRecording()) {

                recordButton.isVisible = true
                pauseRecordingButton.isVisible = false

                playbackButton.isEnabled = true
                cutButton.isEnabled = true
            }
        }
        add(pauseRecordingButton)

        playbackButton.isEnabled = false
        playbackButton.addActionListener {
            if (session.playback()) {
                playbackButton.isVisible = false
                pausePlaybackButton.isVisible = true

                recordButton.isEnabled = false
                cutButton.isEnabled = false

            }
        }
        add(playbackButton)

        pausePlaybackButton.isVisible = false
        pausePlaybackButton.addActionListener {
            if (session.pausePlayback()) {
                pausePlaybackButton.isVisible = false
                playbackButton.isVisible = true

                recordButton.isEnabled = true
                cutButton.isEnabled = true
            }
        }
        add(pausePlaybackButton)

        cutButton.isEnabled = false
        cutButton.addActionListener {
            if (session.isEditSafe) {
                session.makeCut(session.correctedCursor)
            }
        }
        add(cutButton)

    }

    companion object {

        private val PLAY_EMOJI = "▶"
        private val RECORD_EMOJI = "\u23FA"
        private val PAUSE_EMOJI = "❚❚"
        private val STOP_EMOJI = "\u23F9"
        private val SCISSORS_EMOJI = "\u2702"

    }

}