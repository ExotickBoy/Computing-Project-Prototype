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

    val chordController = ChordController()

    /**
     * Makes a cut in the recording by finding the section at the cursors position and splitting it into two sections.
     * It may do nothing if one of the created sections is less than the minimum section length
     * @param time The time to cut the recording at in timesteps
     */
    internal fun cut(time: Int) {

        val cutIndex = sectionAt(time)

        if (cutIndex != null) {

            val cutSection = sections[cutIndex]

            val clusterCut = cutSection.noteClusters.indexOfFirst { cutSection.timeStepStart + it.relTimeStepStart > time }.let {
                if (it == -1) cutSection.noteClusters.size else it
            }

            val left = Section(
                    cutSection.sampleStart,
                    cutSection.timeStepStart,
                    cutSection.clusterStart,
                    cutSection.samples.subList(0, (time - cutSection.timeStepStart) * SAMPLES_BETWEEN_FRAMES + SAMPLE_PADDING),
                    cutSection.timeSteps.subList(0, time - cutSection.timeStepStart),
                    cutSection.noteClusters.subList(0, clusterCut)
            )

            val right = Section(
                    left.sampleEnd,
                    left.timeStepEnd,
                    left.clusterEnd,
                    cutSection.samples.subList((time - cutSection.timeStepStart) * SAMPLES_BETWEEN_FRAMES + SAMPLE_PADDING, cutSection.samples.size),
                    cutSection.timeSteps.subList(time - cutSection.timeStepStart, cutSection.timeSteps.size),
                    cutSection.noteClusters.subList(clusterCut, cutSection.noteClusters.size)
            )

            if (left.timeSteps.size >= Section.minStepLength && right.timeSteps.size >= Section.minStepLength) {

                sections.removeAt(cutIndex)
                sections.add(cutIndex, right)
                sections.add(cutIndex, left)

            }

        }

    }

    internal fun startSection() {

        if (sections.size == 0)
            sections.add(Section(0, 0, 0))
        else
            sections.add(Section(sections.last()))

    }

    /**
     * Finds the section at the time given
     * @param time The time in question
     * @return The index of the section
     */
    internal fun sectionAt(time: Int) = (0 until sections.size)
            .firstOrNull { time <= sections[it].timeStepEnd }

    fun addSamples(newSamples: FloatArray) {
        samples.addAll(newSamples.toTypedArray())
    }

    /**
     * Adds a new time step to the end of the recording
     * @param timeStep The TimeStep that is to be added
     */
    internal fun addTimeStep(timeStep: TimeStep) {

        synchronized(possiblePlacements) {
            // synchronised to prevent concurrent modification

            timeSteps.add(timeStep)

            timeStep.notes.filter { it.pitch in tuning } // the pitch must be within the playable range of the guitar
                    .filter { !notes.contains(it) } // if this note hasn't been added yet
                    .forEach {
                        notes.add(it)
                        possiblePlacements.add(findPlacements(it, tuning))
                        chordController.feed(it)
                    }

            optimiseForward(0) // re-optimise the placements
            //TODO make this more efficient

        }

    }

    /**
     * This optimised the possible placements from a particular placement forward
     * @param from The time at which the optimisation should start
     */
    private fun optimiseForward(from: Int) {

        for (time in from until possiblePlacements.size) {

            val currentPlacements = possiblePlacements[time]

            val nextPaths = if (time == 0) { // no previous paths

                (0 until currentPlacements.size).map {
                    Path(listOf(it), currentPlacements[it].startDistance())
                    // start each path with the starting distance to the placement
                }

            } else {

                val previousPaths = paths[time - 1]

                (0 until currentPlacements.size).map { current ->
                    (0 until previousPaths.size).map { past ->
                        // for each possible pair of the placements in the last time and the current one

                        Path(previousPaths[past].route + current,
                                previousPaths[past].route.mapIndexed { index, place ->
                                    possiblePlacements[index][place] distance currentPlacements[current]
                                }.takeLast(10).sum())

                    }.minBy { it.distance }!! // find the shortest path to current from any past
                }

            }

            if (time < paths.size) { // if this path already exists and needs to be replaced
                paths[time] = nextPaths
            } else {
                paths.add(nextPaths)
            }

        }

        if (paths.size > 0) { // if there is a path
            // TODO debug to find if this is necessary

            val bestPath = paths[paths.size - 1].minBy { it.distance }?.route!!
            // the path with the shortest distance to the last placement

            (from until possiblePlacements.size).forEach {
                // replaces all the placements in the current placement with the best ones
                val currentPlacement = possiblePlacements[it][bestPath[it]]

                if (it < placements.size) {
                    placements[it] = currentPlacement
                } else {
                    placements.add(currentPlacement)
                }
            }

        }

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
                    clusterStart = sections[i - 1].clusterStart + sections[i - 1].noteClusters.size
            )
        }

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
        /**
         * Finds all the placements of a note in a tuning
         * @param note The note to be found for
         * @param tuning The tuning to be searched
         * @return All the possible placements for a tuning
         */
        private fun findPlacements(note: Note, tuning: Tuning): List<Placement> {

            return tuning.strings.mapIndexed { index, it ->
                Placement(note.pitch - it, index, note)
            }.filter {
                it.fret >= 0 && it.fret <= tuning.maxFret
            }

        }

    }

}