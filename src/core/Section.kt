package core

import java.awt.image.BufferedImage
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import javax.imageio.ImageIO

class Section(
        var recording: Recording,
        var sampleStart: Int,
        var timeStepStart: Int,
        var clusterStart: Int,
        var isGathered: Boolean = false,
        var isPreProcessed: Boolean = false,
        var isProcessed: Boolean = false,
        @Transient var samples: MutableList<Float> = mutableListOf(),
        @Transient var clusters: MutableList<NoteCluster> = mutableListOf(),
        @Transient var melImages: MutableList<BufferedImage> = mutableListOf(),
        @Transient var noteImages: MutableList<BufferedImage> = mutableListOf(),
        @Transient var dePhased: MutableList<FloatArray> = mutableListOf(),
        @Transient var dePhasedPower: MutableList<Float> = mutableListOf()
) : Serializable {

    constructor(previous: Section) : this(previous.recording, previous.sampleEnd, previous.timeStepEnd, previous.clusterEnd)

    val timeStepLength
        get() = melImages.map { it.width }.sum()

    val timeStepEnd
        get() = timeStepStart + timeStepLength

    val sampleEnd
        get() = sampleStart + samples.size

    val clusterEnd
        get() = clusterStart + clusters.size

    val timeStepRange
        get() = timeStepStart until timeStepEnd

    val clusterRange
        get() = clusterStart until clusterEnd

    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {

        output.defaultWriteObject()

        output.writeObject(samples.toFloatArray())
        output.writeObject(clusters.toTypedArray())

        ImageIO.write(melImages[0], "png", output)
        ImageIO.write(noteImages[0], "png", output)

        output.writeObject(dePhased.toTypedArray())
        output.writeObject(dePhasedPower.toTypedArray())

    }

    @Throws(ClassNotFoundException::class, IOException::class)
    @Suppress("UNCHECKED_CAST")
    private fun readObject(input: ObjectInputStream) {

        input.defaultReadObject()

        samples = (input.readObject() as FloatArray).toMutableList()
        clusters = (input.readObject() as? Array<NoteCluster>)?.toMutableList() ?: mutableListOf()

        melImages.add(ImageIO.read(input))
        noteImages.add(ImageIO.read(input))

        dePhased = (input.readObject() as Array<FloatArray>).toMutableList()
        dePhasedPower = (input.readObject() as Array<Float>).toMutableList()

    }

    fun addSamples(newSamples: FloatArray) {
        synchronized(recording) {
            samples.addAll(newSamples.toTypedArray())
        }
    }

    companion object {

        const val MIN_STEP_LENGTH = 10

    }

}
