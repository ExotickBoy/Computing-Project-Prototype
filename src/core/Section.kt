package core

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.WritableImage
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import javax.imageio.ImageIO

/**
 * This class stores all of the information of each section of recording
 *
 * @author Kacper Lubisz
 *
 * @property recording the recording the section belongs to
 * @property sampleStart the index at which the samples of this section start at
 * @property timeStepStart the index at which the time steps of this section start
 * @property clusterStart the index at which the time steps of this clusters start
 * @property isGathered if the section has been fully gathered
 * @property isPreProcessed if the section has been preprocessed
 * @property isProcessed if the section has been processed
 * @property samples list of the samples of this section
 * @property clusters list of the note clusters of this section
 * @property melImages list of images of the spectrograms of this section
 * @property noteImages list of images of the raw output of the neural network
 * @property dePhased the de-phased visualisations of the time stapes
 * @property dePhasedPower the volume like value of each step
 */
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

    /**
     * This is an override method which is called when a tuning is being written to file
     */
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
                (Model.MEL_BINS_AMOUNT + Model.PITCH_RANGE),
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

    /**
     * This is an override method which is called when a tuning is being read from file
     */
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

    /**
     * Adds samples to the section
     */
    fun addSamples(newSamples: FloatArray) {
        synchronized(recording) {
            samples.addAll(newSamples.toTypedArray())
        }
    }

    companion object {

        const val MIN_STEP_LENGTH = 10 /*The minimal length a section can be cut to */

    }

}
