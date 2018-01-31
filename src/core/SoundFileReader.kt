package core

import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


internal class SoundFileReader(private val recording: Recording, private val file: File) : Thread("Sound File Reader") {

    lateinit var inputStream: AudioInputStream

    fun open() { // this throws exceptions
        //https://www.ntu.edu.sg/home/ehchua/programming/java/J8c_PlayingSound.html

        inputStream = AudioSystem.getAudioInputStream(file)

        if (inputStream.format.sampleSizeInBits != 16)
            throw UnsupportedBitDepthException()
        if (inputStream.format.channels != 1)
            throw UnsupportedChannelsException()

    }

    override fun run() {

        recording.startSection()

        val read = inputStream.readAllBytes()
        val samples = FloatArray(read.size / 2)

        SoundUtils.bytesToFloats(read, samples, inputStream.format.isBigEndian)

        recording.addSamples(samples)
        recording.endSection()

    }

    internal class UnsupportedBitDepthException : Exception("Only 16 bit files are supported")
    internal class UnsupportedChannelsException : Exception("Only mono supported")

}