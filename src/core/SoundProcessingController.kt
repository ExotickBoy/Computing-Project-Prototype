package core

import javafx.scene.image.WritableImage
import java.util.*

/**
 * This is the thread that is responsible for processing the samples that have already been gathered during the current session
 * Most of the processing occurs inside external functions such as the constructor for timeStep or addTimeStep
 *
 * @author Kacper Lubisz
 *
 * @see TimeStep
 * @see Session
 *
 * @property session The session that this processing controller is for
 * @property timeStepQueue This is the queue of timeSteps that have been preprocessed (fed through spectral analysis
 * and machine learning), these will be served at a constant rate
 * @property bufferThread The private thread responsible for serving the session with the time steps at a constant rate
 */
internal class SoundProcessingController(val session: Session) : Thread(SOUND_PROCESSING_THREAD_TITLE) {

    private val timeStepQueue: LinkedList<TimeStep> = LinkedList()
    private val bufferThread = TimeStepBufferThread(session, timeStepQueue)

    /**
     * Starts the processing
     */
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
                var previousStep: TimeStep? = null

                while (!section.isGathered || processingCursor + FRAME_SIZE < section.samples.size) {

                    if (processingCursor + FRAME_SIZE < section.samples.size) {

                        val newStep = TimeStep(
                                section,
                                processingCursor,
                                if (previousStep?.section != section) null else previousStep
                        )
                        previousStep = newStep
                        synchronized(timeStepQueue) { timeStepQueue }.add(newStep)
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

    /**
     * Makes the thread and the buffer thread stop
     */
    fun end() {
        bufferThread.interrupt()
        interrupt()
    }

    /**
     * Adds a section to the fast precess list which means that it won't be served at a
     * constant rate but instead at max speed
     * @param section the section to be fast processed
     */
    fun fastProcess(section: Section) {

        if (!section.isPreProcessed || !section.isProcessed)
            bufferThread.addToFastProcessing(section)

    }

    companion object {

        private const val SOUND_PROCESSING_THREAD_TITLE = "Sound Processing Thread"

        const val FRAME_RATE = 30
        const val SAMPLE_RATE = 44100
        const val FRAME_SIZE = 1 shl 12
        const val SAMPLES_BETWEEN_FRAMES = SAMPLE_RATE / FRAME_RATE
        const val SAMPLE_PADDING = (FRAME_SIZE - SAMPLES_BETWEEN_FRAMES) / 2

        /**
         * This function takes a list of images and makes them into a list with a
         * single image with all of the images concatenated
         * @param images the list in question
         */
        private fun collectImages(images: MutableList<WritableImage>) {

            if (images.size > 1) {

                val length = images.map { it.width }.sum().toInt()
                val result = WritableImage(length, images[0].height.toInt())

                images.fold(0.0) { acc, new ->
                    drawImage(new, acc.toInt(), 0, result)
                    acc + new.width
                }

                images.clear()
                images.add(result)

            }

        }

        /**
         * This combines images at the end of the list in a way similar to binary counting.
         * I do this because it will reduce the amount of times large images need to be combined.
         * For example, when a recording gets long there would otherwise be a situation where at each time step the
         * large image needs to be combined with one that's one wide.
         * @param images the list in question
         */
        private fun combineImages(images: MutableList<WritableImage>) {

            while (images.size > 1 && images[images.lastIndex - 1].width == images[images.lastIndex].width) {

                val a = images.removeAt(images.lastIndex - 1)
                val b = images.removeAt(images.lastIndex)

                val new = WritableImage(a.width.toInt() * 2, a.height.toInt())

                drawImage(a, 0, 0, new)
                drawImage(b, a.width.toInt(), 0, new)

                images.add(new)

            }

        }

        /**
         * This is a convenience function that copies the contents of one Writable image into another
         */
        private fun drawImage(new: WritableImage, locX: Int, locY: Int, target: WritableImage) {

            (0 until new.width.toInt()).forEach { x ->
                (0 until new.height.toInt()).forEach { y ->

                    target.pixelWriter.setColor(x + locX, y + locY, new.pixelReader.getColor(x, y))

                }
            }

        }

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
     * @property fastProcess This list stores all the sections that should be processed at max speed
     */
    private class TimeStepBufferThread(val session: Session, val queue: LinkedList<TimeStep>)
        : Thread("TimeStepBufferThread") {

        private var fastProcess: MutableList<Section> = mutableListOf()

        /**
         * Adds a section to the fast processing list
         */
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
                    session.recording.processSection() // finds a section to process
                }
                if (section != null) { // waits until there is a section to process
                    val willFastProcess = synchronized(fastProcess) {
                        section in fastProcess
                    }
                    val period = 1000 / FRAME_RATE // timing logic
                    var last = System.currentTimeMillis()
                    var current = last
                    var accumulated = 0.0

                    val patternMatcher = PatternMatcher(section.recording.tuning, section.clusters)
                    // the pattern matcher for finding chords in the current section

                    while (!section.isProcessed) {

                        last = current
                        current = System.currentTimeMillis()
                        accumulated += current - last

                        if (willFastProcess || accumulated > period) {

                            accumulated -= period

                            if (!synchronized(queue) { queue.isEmpty() }) {
                                synchronized(session.recording) {

                                    val newStep: TimeStep = queue.removeFirst() // new step to be served

                                    patternMatcher.feed(newStep.notes)
                                    section.dePhased.add(newStep.dePhased)
                                    section.dePhasedPower.add(newStep.dePhasedPower)

                                    section.melImages.add(newStep.melImage)
                                    section.noteImages.add(newStep.noteImage)
                                    combineImages(section.melImages)
                                    combineImages(section.noteImages)

                                    session.onEdited()
                                    session.onUpdated()
                                    // add time step to recording where further processing happens
                                }
                            } else if (section.isPreProcessed) { // finished
                                synchronized(session.recording) {
                                    collectImages(section.melImages) // collect images
                                    collectImages(section.noteImages)
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