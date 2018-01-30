package components

import core.AppInstance
import core.Session
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

internal class ControlPane(private val session: Session) : JPanel() {

    private val exitButton = JButton(EXIT_BUTTON_TEXT)

    private val recordButton = JButton(RECORD_EMOJI)
    private val pauseRecordingButton = JButton(STOP_EMOJI)
    private val playbackButton = JButton(PLAY_EMOJI)
    private val pausePlaybackButton = JButton(PAUSE_EMOJI)
    private val cutButton = JButton(SCISSORS_EMOJI)

    private val muteButton = JButton(UNMUTED_EMOJI)

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
                    muteButton.isEnabled = true
                }
                session.isRecording -> {

                    recordButton.isVisible = false
                    pauseRecordingButton.isVisible = true
                    playbackButton.isVisible = true
                    pausePlaybackButton.isVisible = false

                    pauseRecordingButton.isEnabled = true
                    playbackButton.isEnabled = false

                    cutButton.isEnabled = false
                    muteButton.isEnabled = false
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
        recordButton.setMnemonic(RECORD_MNEMONIC)
        recordButton.addActionListener {
            if (session.record()) {

                pauseRecordingButton.isVisible = true
                recordButton.isVisible = false

                playbackButton.isEnabled = false
                cutButton.isEnabled = false

            } else {
                recordButton.isEnabled = false
            }
            parent.parent.requestFocusInWindow()
            // pass the focus back onto RecordingEditPane for the key listeners there to work
        }

        pauseRecordingButton.setMnemonic(RECORD_MNEMONIC)
        pauseRecordingButton.isVisible = false
        pauseRecordingButton.addActionListener {
            if (session.pauseRecording()) {

                recordButton.isVisible = true
                pauseRecordingButton.isVisible = false

                playbackButton.isEnabled = true
                cutButton.isEnabled = true
            }
            parent.parent.requestFocusInWindow()
        }

        playbackButton.setMnemonic(PLAY_MNEMONIC)
        playbackButton.isEnabled = !session.recording.isEmpty
        playbackButton.addActionListener {
            if (session.playback()) {
                playbackButton.isVisible = false
                pausePlaybackButton.isVisible = true

                recordButton.isEnabled = false
                cutButton.isEnabled = false
            }
            parent.parent.requestFocusInWindow()
        }

        pausePlaybackButton.setMnemonic(PLAY_MNEMONIC)
        pausePlaybackButton.isVisible = false
        pausePlaybackButton.addActionListener {
            if (session.pausePlayback()) {
                pausePlaybackButton.isVisible = false
                playbackButton.isVisible = true

                recordButton.isEnabled = true
                cutButton.isEnabled = true
            }
            parent.parent.requestFocusInWindow()
        }

        cutButton.setMnemonic(CUT_MNEMONIC)
        cutButton.isEnabled = !session.recording.isEmpty
        cutButton.addActionListener {
            if (session.isEditSafe) {
                session.makeCut(session.correctedStepCursor)
            }
            parent.parent.requestFocusInWindow()
        }

        muteButton.setMnemonic(MUTE_MNEMONIC)
        muteButton.addActionListener {
            val isMuted = session.toggleMute()
            if (isMuted) {
                muteButton.text = MUTED_EMOJI
            } else {
                muteButton.text = UNMUTED_EMOJI
            }
            parent.parent.requestFocusInWindow()
        }

        exitButton.setMnemonic(EXIT_MNEMONIC)
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
        add(muteButton, BorderLayout.EAST)

    }

    companion object {

        private const val PLAY_EMOJI = "▶"
        private const val RECORD_EMOJI = "\u23FA"
        private const val PAUSE_EMOJI = "❚❚"
        private const val STOP_EMOJI = "\u23F9"
        private const val SCISSORS_EMOJI = "\u2702"
        private const val EXIT_BUTTON_TEXT = "Exit"

        private const val MUTED_EMOJI = "🔇"
        private const val UNMUTED_EMOJI = "\uD83D\uDD0A"

        private const val MUTE_MNEMONIC = 'M'
        private const val RECORD_MNEMONIC = 'R'
        private const val PLAY_MNEMONIC = 'P'
        private const val CUT_MNEMONIC = 'C'
        private const val EXIT_MNEMONIC = 'E'


    }

}