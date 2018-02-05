package core

import core.Section.Companion.MIN_STEP_LENGTH
import core.SoundProcessingController.Companion.SAMPLES_BETWEEN_FRAMES
import core.SoundProcessingController.Companion.SAMPLE_PADDING
import core.SoundProcessingController.Companion.SAMPLE_RATE
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.max
import kotlin.math.min


/**
 * Stores all the time steps for a recording.
 * @author Kacper Lubisz
 * @see TimeStep
 * @property tuning The tuning used during this recording
 * @property name The name of the recording that will be displayed to the user
 */
class Recording(val tuning: Tuning, val name: String) : Serializable {

    private val created = System.currentTimeMillis()
    val sections = mutableListOf<Section>()

    val timeStepLength
        get() = if (sections.isEmpty()) 0 else sections.last().timeStepEnd
    val clusterLength
        get() = if (sections.isEmpty()) 0 else sections.last().clusterEnd
    val length: Double
        get() = if (sections.isEmpty()) 0.0 else (sections.last().sampleEnd.toDouble() / SAMPLE_RATE)

    val isEmpty: Boolean
        get() = length == 0.0

    val isGathered: Boolean
        get() = sections.lastOrNull()?.isGathered ?: true

    val isProcessed: Boolean
        get() = sections.lastOrNull()?.isProcessed ?: true

    val isPreProcessed: Boolean
        get() = sections.lastOrNull()?.isPreProcessed ?: true

    internal fun cut(time: Int): Boolean {

        val cutIndex = sectionAt(time)

        return if (cutIndex != null) {

            val cutSection = sections[cutIndex]

            if (time - cutSection.timeStepStart > MIN_STEP_LENGTH && cutSection.timeStepLength - (time - cutSection.timeStepStart) > MIN_STEP_LENGTH) {

                val clusterCut = cutSection.clusters.indexOfFirst { cutSection.timeStepStart + it.relTimeStepStart > time }.let {
                    if (it == -1) cutSection.clusters.size else it
                }

                // this is splitting all the different properties of the section into the left and right one
                val left = Section(
                        this,
                        cutSection.sampleStart,
                        cutSection.timeStepStart,
                        cutSection.clusterStart,
                        true, true, true,
                        cutSection.samples.subList(0, (time - cutSection.timeStepStart) * SAMPLES_BETWEEN_FRAMES + SAMPLE_PADDING),
                        cutSection.clusters.subList(0, clusterCut).toMutableList(),
                        mutableListOf(cutSection.melImages[0].getSubimage(
                                0,
                                0,
                                time - cutSection.timeStepStart,
                                cutSection.melImages[0].height
                        )),
                        mutableListOf(cutSection.noteImages[0].getSubimage(
                                0,
                                0,
                                time - cutSection.timeStepStart,
                                cutSection.noteImages[0].height
                        )),
                        cutSection.dePhased.subList(0, time - cutSection.timeStepStart).toMutableList(),
                        cutSection.dePhasedPower.subList(0, time - cutSection.timeStepStart).toMutableList()
                )

                val right = Section(
                        this,
                        left.sampleEnd,
                        left.timeStepEnd,
                        left.clusterEnd,
                        true, true, true,
                        cutSection.samples.subList((time - cutSection.timeStepStart) * SAMPLES_BETWEEN_FRAMES + SAMPLE_PADDING, cutSection.samples.size).toMutableList(),
                        cutSection.clusters.subList(clusterCut, cutSection.clusters.size).map {
                            it.copy(relTimeStepStart = it.relTimeStepStart - left.timeStepLength)
                        }.toMutableList(),
                        mutableListOf(cutSection.melImages[0].getSubimage(
                                time - cutSection.timeStepStart,
                                0,
                                cutSection.timeStepLength - (time - cutSection.timeStepStart),
                                cutSection.melImages[0].height
                        )),
                        mutableListOf(cutSection.noteImages[0].getSubimage(
                                time - cutSection.timeStepStart,
                                0,
                                cutSection.timeStepLength - (time - cutSection.timeStepStart),
                                cutSection.noteImages[0].height
                        )),
                        cutSection.dePhased.subList(time - cutSection.timeStepStart, cutSection.timeStepLength).toMutableList(),
                        cutSection.dePhasedPower.subList(time - cutSection.timeStepStart, cutSection.timeStepLength).toMutableList()
                )

                synchronized(this) {

                    sections.removeAt(cutIndex)
                    sections.add(cutIndex, right)
                    sections.add(cutIndex, left)

                }

                true

            } else false

        } else false

    }

    internal fun gatherSection(): Section {

        synchronized(this) {

            val section = if (sections.size == 0) {
                Section(this, 0, 0, 0)
            } else {
                Section(sections.last())
            }
            sections.add(section)
            return section

        }

    }

    fun preProcessSection(): Section? = synchronized(this) { sections.firstOrNull { !it.isPreProcessed } }
    fun processSection(): Section? = synchronized(this) { sections.firstOrNull { !it.isProcessed } }

    /**
     * Finds the section at the time given
     * @param time The time in question
     * @return The index of the section
     */
    internal fun sectionAt(time: Int) = (0 until sections.size)
            .firstOrNull { time < sections[it].timeStepEnd }

    /**
     * Swaps two sections of the recording by their incises
     * @param from The index of the first section
     * @param to The index of the second section
     */
    internal fun swapSections(from: Int, to: Int) {

        val temp = sections[from]
        sections[from] = sections[to]
        sections[to] = temp

        relabelStarts(from = min(from, to), to = max(from, to))

    }

    internal fun reInsertSection(from: Int, to: Int) {
        val it = sections[from]
        val corrected = if (to > from) to - 1 else to
        sections.removeAt(from)
        sections.add(corrected, it)

        relabelStarts(from = min(from, to), to = max(from, to))
    }

    internal fun removeSection(sectionIndex: Int) {
        sections.removeAt(sectionIndex)
        if (sections.isNotEmpty() && sections.size != sectionIndex) {
            relabelStarts(from = sectionIndex)
        }
    }

    private fun relabelStarts(from: Int = 0, to: Int = sections.size - 1) {


        sections[from].sampleStart = if (from == 0) 0 else sections[from - 1].sampleEnd
        sections[from].timeStepStart = if (from == 0) 0 else sections[from - 1].timeStepEnd
        sections[from].clusterStart = if (from == 0) 0 else sections[from - 1].clusterEnd

        (from + 1..min(to, sections.size - 1)).forEach { i ->

            sections[i].sampleStart = sections[i - 1].sampleEnd
            sections[i].timeStepStart = sections[i - 1].timeStepEnd
            sections[i].clusterStart = sections[i - 1].clusterEnd

        }

    }

    private fun getMetaData(): RecordingMetaData = RecordingMetaData(name, length, created, System.currentTimeMillis())

    private fun serialize(output: OutputStream) {

        val stream = ObjectOutputStream(if (USE_COMPRESSION) GZIPOutputStream(output) else output)
        stream.writeObject(this.getMetaData())
        stream.writeObject(this)
        stream.close()

    }

    fun save() {

        val start = System.currentTimeMillis()
        serialize(FileOutputStream(File(DEFAULT_PATH + "/" + name + FILE_EXTENSION)))
        println("${System.currentTimeMillis() - start}ms elapsed saving")

    }

    companion object {

        fun deserialize(input: InputStream): Recording {

            val stream = ObjectInputStream(if (USE_COMPRESSION) GZIPInputStream(input) else input)
            stream.readObject() // read and discard the metadata
            val recording = stream.readObject() as Recording
            stream.close()
            return recording

        }

        fun findPossibleRecordings(root: File): List<PossibleRecording> {

            return root.listFiles()?.filter { it.name.endsWith(FILE_EXTENSION) }?.map {
                try {
                    val stream = ObjectInputStream(if (USE_COMPRESSION) GZIPInputStream(FileInputStream(it)) else FileInputStream(it))
                    val metaData = stream.readObject() as RecordingMetaData
                    stream.close() // close the stream without reading the full recording
                    return@map PossibleRecording(it, metaData)
                } catch (e: IOException) {
                    it.delete()
                    return@map null
                }

            }?.filterNotNull() ?: listOf()

        }

        private const val serialVersionUID = 354634135413L // this is used in serializing to make sure class versions match
        private const val FILE_EXTENSION = ".rec"
        const val DEFAULT_PATH = "recordings/"
        const val USE_COMPRESSION = false

    }

    data class RecordingMetaData(val name: String, val length: Double, val created: Long, val lastEdited: Long) : Serializable// metadata object
    data class PossibleRecording(val file: File, val metaData: RecordingMetaData)

}