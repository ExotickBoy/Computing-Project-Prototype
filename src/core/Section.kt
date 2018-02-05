package core

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.WritableImage
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
        @Transient var melImages: MutableList<WritableImage> = mutableListOf(),
        @Transient var noteImages: MutableList<WritableImage> = mutableListOf(),
        @Transient var dePhased: MutableList<FloatArray> = mutableListOf(),
        @Transient var dePhasedPower: MutableList<Float> = mutableListOf()
) : Serializable {

    constructor(previous: Section) : this(previous.recording, previous.sampleEnd, previous.timeStepEnd, previous.clusterEnd)

    val timeStepLength
        get() = melImages.map { it.width }.sum().toInt()

    val timeStepEnd
        get() = timeStepStart + timeStepLength

    val sampleEnd
        get() = sampleStart + samples.size

    val clusterEnd
        get() = clusterStart + clusters.size

    val clusterRange
        get() = clusterStart until clusterEnd

    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {

        output.defaultWriteObject()

        output.writeObject(samples.toFloatArray())
        output.writeObject(clusters.toTypedArray())

        output.writeObject(dePhased.toTypedArray())
        output.writeObject(dePhasedPower.toTypedArray())

        // due to complication I couldn't find an explanation to, I cannot write melImages and noteImages separably,
        // as doing so will result in an exception whenever the second one is read
        // because of this I combine them, this is decremental to save times, but is the best solution I found

        val combined = BufferedImage(
                melImages[0].width.toInt(),
                (Model.MEL_BINS_AMOUNT + Model.PITCH_RANGE).toInt(),
                BufferedImage.TYPE_INT_RGB
        )
        combined.graphics.drawImage(
                SwingFXUtils.fromFXImage(melImages[0], null),
                0, 0,
                null
        )
        combined.graphics.drawImage(
                SwingFXUtils.fromFXImage(noteImages[0], null),
                0, Model.MEL_BINS_AMOUNT,
                null
        )
        ImageIO.write(combined, "PNG", output)

    }

    @Throws(ClassNotFoundException::class, IOException::class)
    @Suppress("UNCHECKED_CAST")
    private fun readObject(input: ObjectInputStream) {

        input.defaultReadObject()

        samples = (input.readObject() as FloatArray).toMutableList()
        clusters = (input.readObject() as? Array<NoteCluster>)?.toMutableList() ?: mutableListOf()

        dePhased = (input.readObject() as Array<FloatArray>).toMutableList()
        dePhasedPower = (input.readObject() as Array<Float>).toMutableList()

        val combined = ImageIO.read(input)
        melImages = mutableListOf(SwingFXUtils.toFXImage(combined.getSubimage(
                0,
                0,
                combined.width,
                Model.MEL_BINS_AMOUNT
        ), null))
        noteImages = mutableListOf(SwingFXUtils.toFXImage(combined.getSubimage(
                0,
                Model.MEL_BINS_AMOUNT,
                combined.width,
                Model.PITCH_RANGE
        ), null))

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
