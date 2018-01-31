package core

import core.Note.Companion.noteLetterShort
import java.io.Serializable

data class Section(
        val recording: Recording,
        val sampleStart: Int,
        val timeStepStart: Int,
        val clusterStart: Int,
        val samples: MutableList<Float> = mutableListOf(),
        val timeSteps: MutableList<TimeStep> = mutableListOf(),
        val clusters: MutableList<NoteCluster> = mutableListOf(),
        var isGathered: Boolean = false,
        var isPreProcessed: Boolean = false,
        var isProcessed: Boolean = false
) : Serializable {

    constructor(after: Section) : this(after.recording, after.sampleEnd, after.timeStepEnd, after.clusterEnd) // new Section

    val timeStepEnd
        get() = timeStepStart + timeSteps.size

    val sampleEnd
        get() = sampleStart + samples.size

    val clusterEnd
        get() = clusterStart + clusters.size

    val timeStepRange
        get() = timeStepStart until timeStepEnd

    val sampleRange
        get() = sampleStart until sampleEnd

    val clusterRange
        get() = clusterStart until clusterEnd

    private val notes: MutableList<Note> = mutableListOf()

    private val states: MutableList<PatternMatchingState> = mutableListOf()
    private val liveStates: MutableList<PatternMatchingState> = mutableListOf()

    private val paths: MutableList<List<Path>> = mutableListOf()
    private val possiblePlacements: MutableList<List<List<Placement>>> = mutableListOf()

    private val chosenMatches: MutableList<PossibleMatch> = mutableListOf()

    fun addSamples(newSamples: FloatArray) {
        samples.addAll(newSamples.toTypedArray())
    }

    fun addTimeStep(timeStep: TimeStep) {
        timeSteps.add(timeStep)

        timeStep.notes.filter { it.pitch in recording.tuning } // the pitch must be within the playable range of the guitar
                .filter { !notes.contains(it) } // if this note hasn't been added yet
                .forEach {
                    notes.add(it)
                    feedPatternMatching(it)
                }

    }

    private fun feedPatternMatching(note: Note) {

        val newState = PatternMatchingState(this, recording.tuning, states.size)
        states.add(newState)
        liveStates.add(newState)

        liveStates.forEach { it.add(note) }
        liveStates.removeIf { it.isDead }

        val new = liveStates.minBy { it.clusterStart }!!
        // choose to add the pattern that started first


        when {
            chosenMatches.isEmpty() -> {
                chosenMatches.add(new.matches.last())
                possiblePlacements.add(chosenMatches[chosenMatches.lastIndex].possiblePlacements)

            }
            chosenMatches[chosenMatches.lastIndex].clusterStart >= new.clusterStart -> {
                chosenMatches[chosenMatches.lastIndex] = new.matches.last()
                possiblePlacements[possiblePlacements.lastIndex] = chosenMatches[chosenMatches.lastIndex].possiblePlacements

                paths.removeAt(paths.lastIndex)
                clusters.removeAt(clusters.lastIndex)

            }
            else -> {
                chosenMatches.add(new.matches.last())
                possiblePlacements.add(chosenMatches[chosenMatches.lastIndex].possiblePlacements)

            }
        }

        (paths.size until possiblePlacements.size).forEach { time ->

            val currentPlacements = possiblePlacements[time]

            val nextPaths = if (time == 0) { // no previous paths

                (0 until currentPlacements.size).map {
                    Path(listOf(it), Placement.internalDistance(currentPlacements[it]))
                    // start each path with the starting distance to the placement
                }

            } else {

                val previousPaths = paths[time - 1]

                (0 until currentPlacements.size).map { to ->
                    (0 until previousPaths.size).map { from ->
                        // for each possible pair of the placements in the last time and the current one

                        Path(previousPaths[from].route + to,
                                previousPaths[from].route.mapIndexed { index, place ->
                                    Placement.distance(possiblePlacements[index][place], currentPlacements[to])
                                }.takeLast(3).sum())

                    }.minBy { it.distance }!! // find the shortest path to current from any past
                }

            }

            paths.add(nextPaths)

        }

        val bestPath = paths.last().minBy { it.distance }?.route!!

        // the path with the shortest distance to the last placement

        (clusters.size until chosenMatches.size).forEach { time ->
            // replaces all the placements in the current placement with the best ones

            val newCluster = NoteCluster(
                    chosenMatches[time].notes.map { it.start }.min() ?: 0-timeStepStart,
                    possiblePlacements[time][bestPath[time]],
                    if (chosenMatches[time].pattern == null) {
                        chosenMatches[time].validRoots.minBy { it.pitch }?.pitch?.noteLetterShort ?: ""
                    } else {
                        (chosenMatches[time].validRoots.map { it.pitch }.min()?.noteLetterShort
                                ?: "") + (chosenMatches[time].pattern?.suffix ?: "")
                    },
                    chosenMatches[time].pattern != null
            )

            clusters.add(newCluster)
        }

    }

    private class PatternMatchingState(val section: Section, val tuning: Tuning, val clusterStart: Int) : Serializable {

        val notes = mutableListOf<Note>()

        var possibleMatches = mutableListOf<PossibleMatch>()
        var isDead = false

        val matches: MutableList<PossibleMatch> = mutableListOf()

        fun add(note: Note) {

            if (notes.isEmpty()) {

                possibleMatches.addAll(patters.map { pattern ->
                    PossibleMatch(note.start - section.timeStepStart, clusterStart, tuning, pattern, mutableListOf(note))
                })
                matches.add(PossibleMatch(note.start - section.timeStepStart, clusterStart, tuning, null, mutableListOf(note)))

            } else {

                if (notes.last().start + MAX_CHORD_SEPARATION < note.start) {
                    possibleMatches.clear()
                }

                possibleMatches.forEach { it.addNote(note) }
                possibleMatches.removeIf { !it.isPossible }

                possibleMatches.filter { it.isValid }.forEach { matches.add(it.copy()) }

                if (possibleMatches.isEmpty()) {
                    isDead = true
                }

            }
            if (clusterStart == 0) {
                println(matches.map {
                    "${it.pattern} + ${it.notes}"
                })
            }
            notes.add(note)
        }

    }

    private data class PossibleMatch constructor(val stepStart: Int, val clusterStart: Int, private val tuning: Tuning, val pattern: ChordPattern?, val notes: MutableList<Note>) : Serializable {

        val isValid: Boolean
            get() = validRoots.isNotEmpty()
        val isPossible: Boolean
            get() = possibleRoots.isNotEmpty()

        val notePlacements: MutableList<List<Placement>> = mutableListOf()
        var placementCombinations: List<List<Int>> = listOf()

        val possiblePlacements: List<List<Placement>>
            get() = placementCombinations.map { list ->
                list.mapIndexed { index, which ->
                    notePlacements[index][which]
                }
            }

        private val possibleRoots: MutableList<Note> = mutableListOf()
        val validRoots: MutableList<Note> = mutableListOf()

        init {
            val startNotes = notes.toList()
            notes.clear()
            startNotes.forEach { addNote(it) }
        }

        internal fun addNote(newNote: Note) {

            val newPlacement = findPossiblePlacements(newNote, tuning)
            notePlacements.add(newPlacement)

            placementCombinations = if (notes.isEmpty()) {
                (0 until newPlacement.size).map { listOf(it) }
            } else {
                placementCombinations.flatMap { existing ->
                    (0 until newPlacement.size).map { new ->
                        existing + new
                    }.filter { list ->
                                Placement.isPossible(list.mapIndexed { index, i -> notePlacements[index][i] }, pattern)
                            }
                }
            }

            possibleRoots.add(newNote)
            possibleRoots.removeIf { !possibleWithRoot(it) }
            validRoots.clear()
            validRoots.addAll(possibleRoots.filter { validWithRoot(it) })

            notes.add(newNote)
        }

        private fun possibleWithRoot(root: Note): Boolean {
            return pattern == null || notes.all {
                (it.pitch - root.pitch).floorMod(12) in pattern.notes
            } && placementCombinations.isNotEmpty()
        }

        private fun validWithRoot(root: Note): Boolean {
            return pattern == null || pattern.notes.none {
                (it + root.pitch) !in notes.map { it.pitch }
            } && placementCombinations.isNotEmpty()
        }

        /**
         * Finds all the placements of a note in a tuning
         * @param note The note to be found for
         * @param tuning The tuning to be searched
         * @return All the possible placements for a tuning
         */
        private fun findPossiblePlacements(note: Note, tuning: Tuning): List<Placement> {

            return tuning.strings.mapIndexed { index, it ->
                Placement(tuning, note.pitch - it, index, note)
            }.filter {
                        it.fret in tuning.capo..tuning.maxFret
                    }

        }

    }

    private data class Path(val route: List<Int>, val distance: Double) : Serializable

    data class ChordPattern(val title: String, val suffix: String, val notes: List<Int>, val maxStringSep: Int?) : Serializable {
        constructor(title: String, suffix: String, vararg notes: Int, maxStringSep: Int? = null)
                : this(title, suffix, notes.asList(), maxStringSep)
    }

    companion object {

        const val MIN_STEP_LENGTH = 10

        val patters = listOf(
                ChordPattern("Major", "M", 0, 4, 7),
                ChordPattern("Minor", "m", 0, 3, 7),
                ChordPattern("Diminished", "d", 0, 3, 6),
                ChordPattern("Augmented", "a", 0, 4, 8),
                ChordPattern("+7", "+7", 0, 7, maxStringSep = 2),
                ChordPattern("Power Chord", "+7", 0, 7, 12, maxStringSep = 3),
                ChordPattern("+5", "+5", 0, 5, maxStringSep = 2)
        )

        const val MAX_CHORD_SEPARATION = 10 // timeSteps

        fun Int.floorMod(other: Int) = ((this % other) + other) % other

    }

}
