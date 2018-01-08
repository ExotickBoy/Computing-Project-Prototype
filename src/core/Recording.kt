package core

/**
 * Stores all the time steps for a recording.
 * @author Kacper Lubisz
 * @see TimeStep
 * @property tuning The tuning used during this recording
 * @property name The name of the recording that will be displayed to the user
 */
class Recording(val tuning: Tuning, val name: String) {

    val samples: MutableList<Float> = mutableListOf()

    val timeSteps: MutableList<TimeStep> = mutableListOf()
    val placements = mutableListOf<Placement>()
    val notes = mutableListOf<Note>()
    val chords get() = chordController.chords
    val sections = mutableListOf<Section>()

    private val paths: MutableList<List<Path>> = mutableListOf()
    private val possiblePlacements: MutableList<List<Placement>> = mutableListOf()
    val chordController = ChordController()

    /**
     * Makes a cut in the recording by finding the section at the cursors position and splitting it into two sections.
     * It may do nothing if one of the created sections is less than the minimum section length
     * @param time The time to cut the recording at
     */
    internal fun cut(time: Int) {

        val cutIndex = sectionAt(time)

        if (cutIndex != null) {


            val cutSection = sections[cutIndex]

            val left = Section(
                    this,
                    cutSection.timeStepStart,
                    cutSection.recordingStart,
                    cutSection.noteStart,
                    cutSection.noteRecordingStart,
                    time - cutSection.recordingStart + 1,
                    cutSection.noteRange.map { (it - cutSection.noteStart) to notes[it] }.firstOrNull { it.second.start - cutSection.timeStepStart + cutSection.recordingStart >= time }?.first ?: cutSection.noteLength
            )
            val right = Section(
                    this,
                    cutSection.timeStepStart + left.correctedLength,
                    cutSection.recordingStart + left.correctedLength,
                    left.noteStart + left.correctedNoteLength,
                    left.noteRecordingStart + left.correctedNoteLength,
                    cutSection.recordingStart + cutSection.correctedLength - time - 1,
                    cutSection.correctedNoteLength - left.correctedNoteLength
            )

            if (left.correctedLength >= Section.minLength && right.correctedLength >= Section.minLength) {

                sections.removeAt(cutIndex)
                sections.add(cutIndex, right)
                sections.add(cutIndex, left)

            }

        }

    }

    internal fun startSection() {

        if (sections.size == 0)
            sections.add(Section(this, 0, 0, 0, 0, null, null))
        else
            sections.add(Section(this, timeSteps.size, timeSteps.size, notes.size, notes.size, null, null))

    }

    internal fun endSection() {

        val last = sections[sections.size - 1]
        sections[sections.size - 1] = last.copy(length = last.correctedLength, noteLength = last.correctedNoteLength)

    }

    /**
     * Finds the section at the time given
     * @param time The time in question
     * @return The index of the section
     */
    internal fun sectionAt(time: Int) = (0 until sections.size)
            .firstOrNull { time <= sections[it].recordingStart + sections[it].correctedLength }

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

        sections[0] = sections[0].copy(recordingStart = 0, noteRecordingStart = 0)
        for (i in 1 until sections.size) {
            sections[i] = sections[i].copy(recordingStart = sections[i - 1].recordingStart + sections[i - 1].correctedLength,
                    noteRecordingStart = sections[i - 1].noteRecordingStart + sections[i - 1].correctedNoteLength)
        }

    }

    internal fun reInsertSection(from: Int, to: Int) {
        val it = sections[from]
        val corrected = if (to > from) to - 1 else to
        sections.removeAt(from)
        sections.add(corrected, it)

        sections[0] = sections[0].copy(recordingStart = 0, noteRecordingStart = 0)
        for (i in 1 until sections.size) {
            sections[i] = sections[i].copy(recordingStart = sections[i - 1].recordingStart + sections[i - 1].correctedLength,
                    noteRecordingStart = sections[i - 1].noteRecordingStart + sections[i - 1].correctedNoteLength)
        }

    }

    companion object {

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

    /**
     * This class is used to store all the possible paths when placements are optimised
     * @property route The list of indices for all the possible placemetns
     * @property distance The distance for the particular path
     */
    private data class Path(val route: List<Int>, val distance: Double)

}