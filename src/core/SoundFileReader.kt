package core

import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


/**
 * This class is responsible for reading sound from files.
 * It is limited to reading only 16 bit mono .wav files
 *
 * @author Kacper Lubisz
 *
 * @property recording the recording the sound will be added to
 * @property file the file that will be read from
 */
internal class SoundFileReader(private val recording: Recording, private val file: File) : Thread(SOUND_READER_THREAD_TITLE) {

    private lateinit var inputStream: AudioInputStream

    /**
     * This opens the input stream that will be read from
     * @throws javax.sound.sampled.UnsupportedAudioFileException when the format isn't supported
     * @throws java.io.IOException when there is an error when opening the stream
     */
    fun open() { // this throws exceptions

        inputStream = AudioSystem.getAudioInputStream(file)

        if (inputStream.format.sampleSizeInBits != 16)
            throw UnsupportedBitDepthException()
        if (inputStream.format.channels != 1)
            throw UnsupportedChannelsException()

    }

    /**
     * When the reader is ran
     */
    override fun run() {

        val section = recording.gatherSection()

        val read = inputStream.readAllBytes()
        val samples = FloatArray(read.size / 2)

        SoundUtils.bytesToFloats(read, samples, inputStream.format.isBigEndian)

        section.addSamples(samples)
        section.isGathered = true

    }


    /**
     * This class is just used to throw exceptions that can be handled in custom ways
     */
    internal class UnsupportedBitDepthException : Exception(BIT_DEPTH_EXCEPTION_MESSAGE)

    /**
     * This class is just used to throw exceptions that can be handled in custom ways
     */
    internal class UnsupportedChannelsException : Exception(CHANNEL_EXCEPTION_MESSAGE)

    companion object {

        private const val BIT_DEPTH_EXCEPTION_MESSAGE = "Only 16 bit files are supported"
        private const val CHANNEL_EXCEPTION_MESSAGE = "Only mono supported"
        private const val SOUND_READER_THREAD_TITLE = "Sound File Reader"

    }

}