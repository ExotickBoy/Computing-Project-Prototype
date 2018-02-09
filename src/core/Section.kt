package core

import java.awt.image.BufferedImage
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.*

class Section(
        val recording: Recording,
        val sampleStart: Int,
        val timeStepStart: Int,
        val clusterStart: Int,
        var isGathered: Boolean = false,
        var isPreProcessed: Boolean = false,
        var isProcessed: Boolean = false,
        @Transient var samples: MutableList<Float> = mutableListOf(),
        @Transient var clusters: MutableList<NoteCluster> = mutableListOf(),
        var melImages: MutableList<BufferedImage> = mutableListOf(),
        var noteImages: MutableList<BufferedImage> = mutableListOf()
) : Serializable {

    @Transient
    val notes: MutableList<Note> = mutableListOf()
    @Transient
    val timeStepQueue: LinkedList<IntRange> = LinkedList()


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

    // the transient keyword means that the serialiser will not serialise it when it is being written to file
    // I can discard all of the pattern matching once if's finished processing
    @Transient
    private val patternMatcher: PatternMatcher? = PatternMatcher(recording.tuning, clusters)

    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {

        output.defaultWriteObject()
        val a = System.currentTimeMillis()

        output.writeObject(samples.toFloatArray())

        val b = System.currentTimeMillis()
        output.writeObject(timeSteps.toTypedArray())
        val c = System.currentTimeMillis()
        output.writeObject(clusters.toTypedArray())

        val end = System.currentTimeMillis()

        println("samples -> ${b - a}")
        println("timeSteps -> ${c - b}")
        println("clusters -> ${end - c}")

    }

    @Throws(ClassNotFoundException::class, IOException::class)
    @Suppress("UNCHECKED_CAST")
    private fun readObject(input: ObjectInputStream) {

        input.defaultReadObject()

        samples = (input.readObject() as FloatArray).toMutableList()
        timeSteps = (input.readObject() as? Array<TimeStep>)?.toMutableList() ?: mutableListOf()
        clusters = (input.readObject() as? Array<NoteCluster>)?.toMutableList() ?: mutableListOf()

        isGathered = true
        isPreProcessed = true
        isProcessed = true

    }

    fun addSamples(newSamples: FloatArray) {
        synchronized(recording) {
            samples.addAll(newSamples.toTypedArray())
        }
    }

    fun preProcessTimeStep(newStep: IntRange) {

        // create images and stuff

        timeStepQueue.add(newStep)

    }

    fun presentTimeStep(newStep: IntRange?) {

        timeStepQueue.remove(newStep)

        val newNotes =

                patternMatcher?.feedNotes(notes)

    }

    private fun getSamples(range: IntRange): FloatArray {
        synchronized(recording) {
            return samples.subList(sampleStart, sampleStart + SoundProcessingController.FRAME_SIZE).toFloatArray()
        }
    }

    private fun addImages(newMelImage: BufferedImage, newNoteImage: BufferedImage) {

        // recombination

    }

    companion object {

        const val MIN_STEP_LENGTH = 10

    }

}
