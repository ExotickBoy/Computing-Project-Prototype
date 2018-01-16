package core

internal class PlaybackController(private val session: Session, private val onEnd: () -> Unit) : Thread("Playback Thread") {

    var isPaused = true
        set(value) {
            field = value
            if (field) {
                playHead = session.correctedStepCursor
            }
        }
    var playHead = 0

    init {
        start()
    }

    override fun run() {

        val period = 1000 / SoundProcessingController.FRAME_RATE
        var last = System.currentTimeMillis();
        var current = last;
        var accumulated = 0.0;

        while (!isInterrupted) {

            last = current;
            current = System.currentTimeMillis();
            accumulated += current - last;

            if (!isPaused) {
                while (accumulated > period) {
                    accumulated -= period;

                    session.stepCursor = session.correctedStepCursor + 1
                    if (session.stepCursor == null) {
                        isPaused = true
                        onEnd.invoke()
                    }

                }
            } else {
                while (isPaused) {
                    onSpinWait()
                }
                current = System.currentTimeMillis()
                last = current
                accumulated = 0.0
            }

        }

    }

}