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
            pauseRecordingButton.isVisible = true
            recordButton.isVisible = false

            playbackButton.isEnabled = false
            cutButton.isEnabled = false

            session.record()

        }
        add(recordButton)

        pauseRecordingButton.isVisible = false
        pauseRecordingButton.addActionListener {
            recordButton.isVisible = true
            pauseRecordingButton.isVisible = false

            playbackButton.isEnabled = true
            cutButton.isEnabled = true

            session.pauseRecording()
        }
        add(pauseRecordingButton)

        playbackButton.isEnabled = false
        playbackButton.addActionListener {
            playbackButton.isVisible = false
            pausePlaybackButton.isVisible = true

            recordButton.isEnabled = false
            cutButton.isEnabled = false

            session.playback()

        }
        add(playbackButton)

        pausePlaybackButton.isVisible = false
        pausePlaybackButton.addActionListener {
            pausePlaybackButton.isVisible = false
            playbackButton.isVisible = true

            recordButton.isEnabled = true
            cutButton.isEnabled = true

            session.pausePlayback()

        }
        add(pausePlaybackButton)

        cutButton.isEnabled = false
        cutButton.addActionListener {
            if (session.cursor != -1) {
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