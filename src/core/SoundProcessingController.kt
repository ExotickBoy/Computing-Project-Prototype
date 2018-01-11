package core

import java.util.*

internal class SoundProcessingController(val session: Session) : Thread("Sound Processing Thread") {

    val isPaused = true

    private val timeStepQueue: LinkedList<TimeStep> = LinkedList()
    private val bufferThread = TimeStepBufferThread(session, timeStepQueue)

    init {

        bufferThread.start()
        start()

    }

    override fun run() {

        var currentPosition = 0

        while (!isInterrupted) {
            println("PROCESSING $currentPosition ${session.recording.samples.size}")

            if (!isPaused && session.recording.samples.size - currentPosition > FRAME_SIZE) { // new frame

                val newStep = TimeStep(
                        session.recording,
                        currentPosition until currentPosition + FRAME_SIZE,
                        session.recording.timeSteps.last()
                )
                timeStepQueue.add(newStep)

                currentPosition += SAMPLES_BETWEEN_FRAMES
            }

        }

    }

    companion object {

        const val FRAME_RATE = 30
        private const val SAMPLE_RATE = 44100;
        private const val FRAME_SIZE = 1 shl 12;
        private const val SAMPLES_BETWEEN_FRAMES = SAMPLE_RATE / FRAME_RATE

    }

    private class TimeStepBufferThread(val session: Session, val queue: LinkedList<TimeStep>)
        : Thread("TimeStepBufferThread") {

        override fun run() {

            val period = 1000 / FRAME_RATE
            var last = System.currentTimeMillis();
            var current = last;
            var accumulated = 0.0;

            while (!(isInterrupted && queue.isEmpty())) {

                last = current;
                current = System.currentTimeMillis();
                accumulated += current - last;

                while (accumulated > period) {
                    accumulated -= period;

                    if (!queue.isEmpty()) {
                        session.addTimeStep(queue.removeFirst())
                    } // else flag up slow performance

                }

            }

        }

    }

}