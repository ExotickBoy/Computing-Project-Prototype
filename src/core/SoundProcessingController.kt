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

        var previousStep: TimeStep? = null

        while (!isInterrupted) {

            val section = session.recording.lastSection()
            if (section != null && section.processingCursor + FRAME_SIZE <= section.samples.size) { // new frame

                val newStep = TimeStep(
                        session.recording.sections.last(),
                        section.processingCursor until section.processingCursor + FRAME_SIZE,
                        previousStep
                )
                previousStep = newStep
                timeStepQueue.add(newStep)
                section.processingCursor += SAMPLES_BETWEEN_FRAMES
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