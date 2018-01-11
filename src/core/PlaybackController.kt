package core

internal class PlaybackController(private val session: Session, private val onEnd: () -> Unit) : Thread("Playback Thread") {

    var isPaused = true
        set(value) {
            field = value
            if (field) {
                playHead = session.correctedCursor
            }
        }
    var playHead = 0

    override fun run() {

        while (!isInterrupted) {

            if (!isPaused) {

                // play sound back

                session.cursor = session.correctedCursor + 1
                if (session.cursor == null) {
                    isPaused = true
                    onEnd.invoke()
                }

            }

        }

    }

}