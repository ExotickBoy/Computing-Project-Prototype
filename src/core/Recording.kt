package core

import core.SoundProcessingController.Companion.SAMPLES_BETWEEN_FRAMES
import core.SoundProcessingController.Companion.SAMPLE_PADDING
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


/**
 * Stores all the time steps for a recording.
 * @author Kacper Lubisz
 * @see TimeStep
 * @property tuning The tuning used during this recording
 * @property name The name of the recording that will be displayed to the user
 */
class Recording(val tuning: Tuning, val name: String) : Serializable {

    val sections = mutableListOf<Section>()

    val timeStepLength
        get() = if (sections.isEmpty()) 0 else sections.last().timeStepEnd
    val clusterLength
        get() = if (sections.isEmpty()) 0 else sections.last().clusterEnd


    /**
     * Makes a cut in the recording by finding the section at the cursors position and splitting it into two sections.
     * It may do nothing if one of the created sections is less than the minimum section length
     * @param time The time to cut the recording at in timesteps
     */
    internal fun cut(time: Int) {

        val cutIndex = sectionAt(time)

        if (cutIndex != null) {

            val cutSection = sections[cutIndex]

            val clusterCut = cutSection.clusters.indexOfFirst { cutSection.timeStepStart + it.relTimeStepStart > time }.let {
                if (it == -1) cutSection.clusters.size else it
            }

            val left = Section(
                    this,
                    cutSection.sampleStart,
                    cutSection.timeStepStart,
                    cutSection.clusterStart,
                    cutSection.samples.subList(0, (time - cutSection.timeStepStart) * SAMPLES_BETWEEN_FRAMES + SAMPLE_PADDING),
                    cutSection.timeSteps.subList(0, time - cutSection.timeStepStart),
                    cutSection.clusters.subList(0, clusterCut),
                    true, true
            )

            val right = Section(
                    this,
                    left.sampleEnd,
                    left.timeStepEnd,
                    left.clusterEnd,
                    cutSection.samples.subList((time - cutSection.timeStepStart) * SAMPLES_BETWEEN_FRAMES + SAMPLE_PADDING, cutSection.samples.size),
                    cutSection.timeSteps.subList(time - cutSection.timeStepStart, cutSection.timeSteps.size),
                    cutSection.clusters.subList(clusterCut, cutSection.clusters.size),
                    true, true
            )

            if (left.timeSteps.size >= Section.MIN_STEP_LENGTH && right.timeSteps.size >= Section.MIN_STEP_LENGTH) {

                sections.removeAt(cutIndex)
                sections.add(cutIndex, right)
                sections.add(cutIndex, left)

            }

        }

    }

    internal fun startSection() {

        if (sections.size == 0)
            sections.add(Section(this, 0, 0, 0))
        else
            sections.add(Section(sections.last()))

    }

    fun endSection() {
        sections.last().isGathered = true
    }

    /**
     * Finds the section at the time given
     * @param time The time in question
     * @return The index of the section
     */
    internal fun sectionAt(time: Int) = (0 until sections.size)
            .firstOrNull { time < sections[it].timeStepEnd }

    fun addSamples(samples: FloatArray) {
        sections.last().addSamples(samples)
    }

    /**
     * Adds a new time step to the end of the recording
     * @param timeStep The TimeStep that is to be added
     */
    internal fun addTimeStep(timeStep: TimeStep) {
        sections.last().addTimeStep(timeStep)
    }

    /**
     * Swaps two sections of the recording by their incises
     * @param a The index of the first section
     * @param b The index of the second section
     */
    internal fun swapSections(a: Int, b: Int) {

        val temp = sections[a]
        sections[a] = sections[b]
        sections[b] = temp

        sections[0] = sections[0].copy(sampleStart = 0, timeStepStart = 0, clusterStart = 0)
        for (i in 1 until sections.size) {
            sections[i] = sections[i].copy(
                    sampleStart = sections[i - 1].sampleEnd,
                    timeStepStart = sections[i - 1].timeStepEnd,
                    clusterStart = sections[i - 1].clusterEnd
            )
        }

    }

    internal fun reInsertSection(from: Int, to: Int) {
        val it = sections[from]
        val corrected = if (to > from) to - 1 else to
        sections.removeAt(from)
        sections.add(corrected, it)

        sections[0] = sections[0].copy(sampleStart = 0, timeStepStart = 0, clusterStart = 0)
        for (i in 1 until sections.size) {
            sections[i] = sections[i].copy(
                    sampleStart = sections[i - 1].sampleStart + sections[i - 1].samples.size,
                    timeStepStart = sections[i - 1].timeStepStart + sections[i - 1].timeSteps.size,
                    clusterStart = sections[i - 1].clusterStart + sections[i - 1].clusters.size
            )
        }

    }

    private fun optimiseSections() {

    }

    private fun optimiseLastSection() {

    }

    fun serialize(output: OutputStream, willCompress: Boolean = DEFAULT_WILL_COMPRESS) {

        output.write(if (willCompress) 1 else 0)
        val stream = ObjectOutputStream(if (willCompress) GZIPOutputStream(output) else output)
        stream.writeObject(this)

    }

    companion object {

        fun deserialize(input: InputStream): Recording {
            val isCompressed = input.read() == 1
            val stream = ObjectInputStream(if (isCompressed) GZIPInputStream(input) else input)

            return stream.readObject() as Recording

        }

        private val serialVersionUID = 354634135413L; // this is used in serializing to make sure class versions match
        private const val DEFAULT_WILL_COMPRESS = true

    }

    fun lastSection(): Section? = if (sections.isEmpty()) null else sections.last()

}