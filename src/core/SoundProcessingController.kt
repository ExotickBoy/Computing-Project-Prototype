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

    private val timeStepQueue: LinkedList<IntRange> = LinkedList()
    private val bufferThread = TimeStepBufferThread(session, timeStepQueue)

    fun begin() {

        bufferThread.start()
        start()

    }

    /**
     * When the thread is started
     */
    override fun run() {

        while (!isInterrupted) { // until the thread is stopped

            val section = synchronized(session.recording) {
                session.recording.preProcessSection()
            }
            if (section != null) {
                var processingCursor = 0

                while (!section.isGathered || processingCursor + FRAME_SIZE < section.samples.size) {

                    if (processingCursor + FRAME_SIZE < section.samples.size) {

                        val newStep = processingCursor..(processingCursor + FRAME_SIZE)
                        synchronized(timeStepQueue) { timeStepQueue }.add(newStep)
                        section.preProcessTimeStep(newStep)
                        processingCursor += SAMPLES_BETWEEN_FRAMES
                    } else {
                        onSpinWait()
                    }
                }

                section.isPreProcessed = true
                session.onUpdated()

            } else {
                onSpinWait()
            }

        }

    }

    fun end() {
        bufferThread.interrupt()
        interrupt()
    }

    fun fastProcess(section: Section) {

        if (!section.isPreProcessed || !section.isProcessed)
            bufferThread.addToFastProcessing(section)

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
    private class TimeStepBufferThread(val session: Session, val queue: LinkedList<IntRange>)
        : Thread("TimeStepBufferThread") {

        private var fastProcess: MutableList<Section> = mutableListOf()

        fun addToFastProcessing(section: Section) {
            synchronized(fastProcess) {
                fastProcess.add(section)
            }
        }

        /**
         * When the thread is started
         */
        override fun run() {

            while (!(isInterrupted && queue.isEmpty())) { // it must output the data is is given before it stops

                val section = synchronized(session.recording) {
                    session.recording.processSection()
                }
                if (section != null) {
                    val willFastProcess = synchronized(fastProcess) {
                        section in fastProcess
                    }
                    val period = 1000 / FRAME_RATE // timing logic
                    var last = System.currentTimeMillis()
                    var current = last
                    var accumulated = 0.0

                    while (!section.isProcessed) {

                        last = current
                        current = System.currentTimeMillis()
                        accumulated += current - last

                        if (willFastProcess || accumulated > period) {

                            accumulated -= period

                            if (!synchronized(queue) { queue }.isEmpty()) {
                                synchronized(session.recording) {
                                    section.presentTimeStep(queue.removeFirst())
                                    session.onEdited()
                                    session.onUpdated()
                                    // add time step to recording where further processing happens
                                }
                            } else if (section.isPreProcessed) {
                                synchronized(session.recording) {
                                    section.isProcessed = true
                                }
                            } else {
                                onSpinWait()
                            }

                        }

                    }
                    session.onUpdated()
                }

            }

        }

    }

}