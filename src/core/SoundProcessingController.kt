package core

import java.util.*

/**
 * This is the thread that is responsible for samples that have already been gathered during the current session
 * Most of the processing occurs inside external functions such as the constructor for timeStep or addTimeStep
 *
 * @author Kacper Lubisz
 *
 * @see TimeStep
 * @see Session
 *
 * @property session The session that this processing controller is for
 * @property timeStepQueue This is the queue of timeSteps that have been preprocessed (fed through spectral analysis
 * and machine learning), these will be served to the session at a constant rate
 * @property bufferThread The private thread responsible for serving the session with the time steps at a constant rate
 * @property isProcessing This shows if the controller is currently processing some sound
 */
internal class SoundProcessingController(val session: Session) : Thread("Sound Processing Thread") {

    private val timeStepQueue: LinkedList<TimeStep> = LinkedList()
    private val bufferThread = TimeStepBufferThread(session, timeStepQueue)
    var isProcessing = false

    init {

        bufferThread.start()
        start()

    }

    /**
     * When the thread is started
     */
    override fun run() {

        var previousStep: TimeStep? = null
        var processingCursor = 0

        while (!isInterrupted) { // until the thread is stopped

            synchronized(session.recording) {

                val section = session.recording.lastSection()
                if (section != null && !section.isProcessed && processingCursor + FRAME_SIZE <= section.samples.size) { // new frame

                    isProcessing = true
                    val newStep = TimeStep(
                            section,
                            processingCursor,
                            if (previousStep?.section != section) null else previousStep
                    )

                    previousStep = newStep
                    timeStepQueue.add(newStep)
                    processingCursor += SAMPLES_BETWEEN_FRAMES

                } else {
                    if (section != null && section.isGathered) {
                        section.isProcessed = true
                        processingCursor = 0
                    }
                    isProcessing = false
                }

            }

            if (!isProcessing) {
                onSpinWait()
            }

        }

    }

    fun end() {
        bufferThread.interrupt()
        interrupt()
    }

    companion object {

        const val FRAME_RATE = 30
        const val SAMPLE_RATE = 44100
        const val FRAME_SIZE = 1 shl 12
        const val SAMPLES_BETWEEN_FRAMES = SAMPLE_RATE / FRAME_RATE
        const val SAMPLE_PADDING = (FRAME_SIZE - SAMPLES_BETWEEN_FRAMES) / 2

    }

    /**
     * This thread is responsible for serving the TimeSteps from the queue to the session where they are further
     * processed in this thread
     *
     * @author Kacper Lubisz
     *
     * @see TimeStep
     * @see SoundProcessingController
     *
     * @property session The session the timeStep is to be added to
     * @property queue The mutable queue of TimeSteps which is served from
     */
    private class TimeStepBufferThread(val session: Session, val queue: LinkedList<TimeStep>)
        : Thread("TimeStepBufferThread") {

        /**
         * When the thread is started
         */
        override fun run() {

            val period = 1000 / FRAME_RATE // timing logic
            var last = System.currentTimeMillis()
            var current = last
            var accumulated = 0.0

            while (!(isInterrupted && queue.isEmpty())) { // it must output the data is is given before it stops

                last = current
                current = System.currentTimeMillis()
                accumulated += current - last

                while (accumulated > period) {
                    accumulated -= period

                    if (!queue.isEmpty()) {
                        session.addTimeStep(queue.removeFirst()) // add time step to recording for further processing
                    } // else flag up slow performance

                }

            }

        }

    }

}