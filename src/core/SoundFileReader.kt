package core

import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


internal class SoundFileReader(private val recording: Recording, private val file: File) : Thread("Sound File Reader") {

    lateinit var inputStream: AudioInputStream

    fun open() { // this throws exceptions

        inputStream = AudioSystem.getAudioInputStream(file)

        if (inputStream.format.sampleSizeInBits != 16)
            throw UnsupportedBitDepthException()
        if (inputStream.format.channels != 1)
            throw UnsupportedChannelsException()

    }

    override fun run() {

        val section = recording.gatherSection()

        val read = inputStream.readAllBytes()
        val samples = FloatArray(read.size / 2)

        SoundUtils.bytesToFloats(read, samples, inputStream.format.isBigEndian)

        section.addSamples(samples)
        section.isGathered = true

    }

    internal class UnsupportedBitDepthException : Exception("Only 16 bit files are supported")
    internal class UnsupportedChannelsException : Exception("Only mono supported")

}