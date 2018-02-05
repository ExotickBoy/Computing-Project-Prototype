package core

import core.Section.Companion.MIN_STEP_LENGTH
import core.SoundProcessingController.Companion.SAMPLES_BETWEEN_FRAMES
import core.SoundProcessingController.Companion.SAMPLE_PADDING
import core.SoundProcessingController.Companion.SAMPLE_RATE
import javafx.scene.image.WritableImage
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.max
import kotlin.math.min


/**
 * Stores all information relevant to a recording
 *
 * @author Kacper Lubisz
 *
 * @see TimeStep
 *
 * @property tuning The tuning used during this recording
 * @property name The name of the recording that will be displayed to the user
 * @property created The unix timestamp of when the recording was created
 * @property sections the list of sections which make up this recording
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

    /**
     * Splits the section at the input time
     * @param time the time at which the cut should be made
     * @return if the cut was successful
     */
    internal fun cut(time: Int): Boolean {

        val cutIndex = sectionAt(time)
        // index of th section to be cut
        return if (cutIndex != null) {

            val cutSection = sections[cutIndex]

            if (time - cutSection.timeStepStart > MIN_STEP_LENGTH && cutSection.timeStepLength - (time - cutSection.timeStepStart) > MIN_STEP_LENGTH) {
                // if each, the left and righ, are long enough

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
                        mutableListOf(cutSection.melImages[0].getSubImage(
                                0,
                                0,
                                time - cutSection.timeStepStart,
                                cutSection.melImages[0].height.toInt()
                        )),
                        mutableListOf(cutSection.noteImages[0].getSubImage(
                                0,
                                0,
                                time - cutSection.timeStepStart,
                                cutSection.noteImages[0].height.toInt()
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
                        mutableListOf(cutSection.melImages[0].getSubImage(
                                time - cutSection.timeStepStart,
                                0,
                                cutSection.timeStepLength - (time - cutSection.timeStepStart),
                                cutSection.melImages[0].height.toInt()
                        )),
                        mutableListOf(cutSection.noteImages[0].getSubImage(
                                time - cutSection.timeStepStart,
                                0,
                                cutSection.timeStepLength - (time - cutSection.timeStepStart),
                                cutSection.noteImages[0].height.toInt()
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

    /**
     * Starts gathering a new section
     * @return The new section that is to be gathered
     */
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

    /**
     * Finds a section that needs to be preprocessed
     * @return the section to be preprocessed, null if all are already preprocessed
     */
    fun preProcessSection(): Section? = synchronized(this) { sections.firstOrNull { !it.isPreProcessed } }

    /**
     * Finds a section that needs to be processed
     * @return the section to be preprocessed, null if all are already processed
     */
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

    /**
     * This is when the section is being swapped and it is placed in between two other sections.
     * This means that the section is sloted in between other ones
     * @from the index from which the section will be moved
     * @to the index of the split between sections that the from section should be put into
     */
    internal fun reInsertSection(from: Int, to: Int) {
        val it = sections[from]
        val corrected = if (to > from) to - 1 else to
        sections.removeAt(from)
        sections.add(corrected, it)

        relabelStarts(from = min(from, to), to = max(from, to))
    }

    /**
     * Removes a section
     * @param sectionIndex The index of the section to be removed
     */
    internal fun removeSection(sectionIndex: Int) {
        sections.removeAt(sectionIndex)
        if (sections.isNotEmpty() && sections.size != sectionIndex) {
            relabelStarts(from = sectionIndex)
        }
    }

    /**
     * This refreshes the start values of all the sections so that they are all correct
     * @param from the first section that needs refreshing
     * @param to the last section that needs refreshing
     */
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

    /**
     * Makes a metadata object that represents the recording
     * @return The metadata object
     */
    private fun getMetaData(): RecordingMetaData = RecordingMetaData(name, length, created, System.currentTimeMillis())

    /**
     * Writes the recording along with metadata to the output stream
     * @param output An output stream that the recording is to be written to
     */
    private fun serialize(output: OutputStream) {

        val stream = ObjectOutputStream(if (USE_COMPRESSION) GZIPOutputStream(output) else output)
        stream.writeObject(this.getMetaData())
        stream.writeObject(this)
        stream.close()

    }

    /**
     * This opens a file and then serialises the recording to it
     */
    fun save() {

//        val start = System.currentTimeMillis()
        serialize(FileOutputStream(File(DEFAULT_PATH + "/" + name + FILE_EXTENSION)))
//        println("${System.currentTimeMillis() - start}ms elapsed saving")

    }

    companion object {

        /**
         * This reads a recording from a stream
         * @input The input stream that is to be read from
         * @return the recording object that is read
         */
        fun deserialize(input: InputStream): Recording {

            val stream = ObjectInputStream(if (USE_COMPRESSION) GZIPInputStream(input) else input)
            stream.readObject() // read and discard the metadata
            val recording = stream.readObject() as Recording
            stream.close()
            return recording

        }

        /**
         * Finds the metadata of all of the recordings found in the specified directory
         * @root the directory that will be searched for recording
         */
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
        const val DEFAULT_NAME = "Recording"
        const val DEFAULT_PATH = "recordings/"
        const val USE_COMPRESSION = false

        /**
         * A convenience extension method that extracts a section of image
         * @param locX the start x location of the new image
         * @param locY the start x location of the new image
         * @param width the width of the new image
         * @param height the height of the new image
         * @return the new image cut from the bigger image
         */
        private fun WritableImage.getSubImage(locX: Int, locY: Int, width: Int, height: Int): WritableImage {

            val result = WritableImage(width, height)

            (0 until width).forEach { x ->
                (0 until height).forEach { y ->
                    result.pixelWriter.setColor(x, y, this.pixelReader.getColor(locX + x, locY + y))
                }
            }

            return result

        }
    }

    /**
     * A data class that stores the metadata of a recording
     */
    data class RecordingMetaData(val name: String, val length: Double, val created: Long, val lastEdited: Long) : Serializable// metadata object

    /**
     * A data class used to store the file and metadata of a possible recording such that it can be read from later
     */
    data class PossibleRecording(val file: File, val metaData: RecordingMetaData)

}