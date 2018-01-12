package core

import java.util.*

internal class SoundProcessingController(val session: Session) : Thread("Sound Processing Thread") {

    private val timeStepQueue: LinkedList<TimeStep> = LinkedList()
    private val bufferThread = TimeStepBufferThread(session, timeStepQueue)

    init {

        bufferThread.start()
        start()

    }

    override fun run() {

        var currentPosition = 0
        var previous: TimeStep? = null

        while (!isInterrupted) {

//            println("$currentPosition ${session.recording.sections} ${session.recording.lastSection()?.samples?.size}")

            val section = session.recording.lastSection()
            if (currentPosition + FRAME_SIZE - (section?.sampleStart ?: 0) <= section?.samples?.size ?: 0) { // new frame

                println("making a step with ${currentPosition - (section?.sampleStart ?: 0) until currentPosition + FRAME_SIZE - (section?.sampleStart ?: 0)}")

                val newStep = TimeStep(
                        session.recording.sections.last(),
                        currentPosition - (section?.sampleStart ?: 0) until currentPosition + FRAME_SIZE - (section?.sampleStart ?: 0),
                        previous
                )
                previous = newStep
                timeStepQueue.add(newStep)
                currentPosition += SAMPLES_BETWEEN_FRAMES
            }
        }

    }

    companion object {

        const val FRAME_RATE = 30
        private const val SAMPLE_RATE = 44100;
        private const val FRAME_SIZE = 1 shl 12;
        const val SAMPLES_BETWEEN_FRAMES = SAMPLE_RATE / FRAME_RATE
        const val SAMPLE_PADDING = FRAME_SIZE - SAMPLES_BETWEEN_FRAMES
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