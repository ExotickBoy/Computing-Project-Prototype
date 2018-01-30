package core

import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem


internal class SoundFileReader(private val recording: Recording, private val file: File) : Thread("Sound File Reader") {

    lateinit var inputStream: AudioInputStream;

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

        // recording -> PCM_SIGNED 44100.0 Hz, 16 bit, stereo, 4 bytes/frame, little-endian
        // microphone -> PCM_SIGNED 44100.0 Hz, 16 bit, mono, 2 bytes/frame, big-endian

        val read = ByteArray(inputStream.frameLength.toInt() * inputStream.format.sampleSizeInBits / 8)
        val samples = FloatArray(read.size / 2)

        while (!isInterrupted) {

            val bytesRead = inputStream.read(read, 0, read.size)
            SoundUtils.bytesToFloats(read, samples, inputStream.format.isBigEndian)
            if (bytesRead == -1)
                break
            else if (bytesRead == read.size)
                recording.addSamples(samples.sliceArray(0 until bytesRead))
            else
                recording.addSamples(samples.slice(0 until bytesRead).toFloatArray())

        }

    }


    internal class UnsupportedBitDepthException : Exception("Only 16 bit files are supported")
    internal class UnsupportedChannelsException : Exception("Only mono supported")

}