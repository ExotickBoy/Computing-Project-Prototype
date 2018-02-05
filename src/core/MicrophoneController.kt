package core

import javax.sound.sampled.*

/**
 * This class is responsible for interfacing with the microphone.
 * It reads the samples from the source and then passes them to the session to deal with
 *
 * @author Kacper Lubisz
 *
 * @property session The session that the controller is for
 * @property targetLine The microphone input line
 * @property isPaused Whether the microphone is currently recording ir not
 */
internal class MicrophoneController(val session: Session) : Thread("Microphone Thread") {

    private var targetLine: TargetDataLine? = null

    @Volatile // so that each time isPaused is accessed it is read from main memory, this prevents a thread caching it
    var isPaused = true

    /**
     * @return whether the thread already has an open stream
     */
    val isOpen: Boolean
        get() = targetLine?.isOpen ?: false

    /**
     * This opens the microphone line if one is being used and starts the thread
     */
    fun begin() {

        openMicrophoneStream()
        start()
    }

    /**
     * Permanently stops the thread from running by closing the input line
     */
    fun end() {
        targetLine?.close()
    }

    /**
     * Opens the default microphone
     *
     * @throws IllegalArgumentException If the default parameters of audio format being used aren't possible
     * given the microphone
     * @throws LineUnavailableException If a microphone line cannot be opened because a microphone isn't connected
     */
    private fun openMicrophoneStream() {

        val format = AUDIO_FORMAT
        val targetInfo = DataLine.Info(TargetDataLine::class.java, format)

        targetLine = AudioSystem.getLine(targetInfo) as TargetDataLine
        targetLine!!.open(format, SAMPLE_BUFFER_SIZE * 2)
        targetLine!!.start()

    }

    /**
     * When the thread is started
     */
    override fun run() {

        var numBytesRead: Int
        val read = ByteArray(targetLine!!.bufferSize)
        val samples = FloatArray(targetLine!!.bufferSize / 2)
        // two bytes to each sample

        while (isOpen) { // this thread stops when the microphone line is stopped

            val section = synchronized(session.recording) { session.recording.gatherSection() }

            while (!isPaused) {

                numBytesRead = targetLine!!.read(read, 0, read.size)
                if (numBytesRead == -1)
                    break // this will only happen if there is a problem with the input stream, i.e. microphone is disconnected

                SoundUtils.bytesToFloats(read, samples, targetLine!!.format.isBigEndian)
                synchronized(session.recording) {
                    section.addSamples(samples)
                }
                session.onEdited()

            }

            section.isGathered = true
            session.onUpdated()

            while (isPaused) {
                targetLine!!.read(read, 0, read.size)
                onSpinWait()
            }
        }

    }

    companion object {

        const val SAMPLE_RATE = 44100
        const val SAMPLE_BUFFER_SIZE = 2205
        val AUDIO_FORMAT = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, true)

    }

}
