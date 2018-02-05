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

    val clusterRange
        get() = clusterStart until clusterEnd

    @Transient
    private val notes: MutableList<Note> = mutableListOf()

    private val liveStates: MutableList<PatternMatchingState> = mutableListOf()
    @Transient
    private val chosenMatches: MutableList<PossibleMatch> = mutableListOf()

    @Transient
    private val paths: MutableList<List<Path>> = mutableListOf()

    fun addSamples(newSamples: FloatArray) {
        synchronized(recording) {
            samples.addAll(newSamples.toTypedArray())
        }
    }

    fun addTimeStep(timeStep: TimeStep) {
        timeSteps.add(timeStep)

        timeStep.notes.filter { it.pitch in recording.tuning } // the pitch must be within the playable range of the guitar
                .filter { it !in notes.takeLastWhile { it.start <= it.start } } // if this note hasn't been added yet
                .forEach {
                    notes.add(it)
                    feedPatternMatching(it)
                }

    }

    private fun feedPatternMatching(note: Note) {

        val newState = PatternMatchingState(recording.tuning, chosenMatches.size)
        liveStates.add(newState)

        liveStates.forEach { it.add(note) }
        liveStates.removeIf { it.isDead }

        val newMatch = liveStates.minBy { it.clusterStart }!!.matches.last()
        // choose to state that has the pattern that started first, this means it will choose the cord with most notes
        // this will never be null since each new state will always have one match

        when {
            chosenMatches.isEmpty() || chosenMatches[chosenMatches.lastIndex].clusterStart < newMatch.clusterStart -> {
                chosenMatches.add(newMatch)
            }
            else -> {
                paths.removeAt(paths.lastIndex)
                clusters.removeAt(clusters.lastIndex)
                chosenMatches.removeAt(chosenMatches.lastIndex)

                chosenMatches.add(newMatch)
            }
        }

        (paths.size until chosenMatches.size).forEach { time ->

            val nextPlacements = chosenMatches[time].possiblePlacement

            val nextPaths = if (time == 0) { // no previous paths

                (0 until nextPlacements.size).map {
                    Path(listOf(it), Placement.internalDistance(nextPlacements[it]))
                    // start each path with the starting distance of the placement
                }

            } else {

                val previousPlacements = chosenMatches[time - 1].possiblePlacement
                val previousPaths = paths[time - 1]
                // the best paths to each of the previous placements

                (0 until nextPlacements.size).map { nextPlacementIndex ->
                    // index of the placement to be evaluated
                    (0 until previousPlacements.size).map { previousPlacementIndex ->
                        // for each possible pair of the placements in the last time and the current one

                        Path(previousPaths[previousPlacementIndex].route + nextPlacementIndex,
                                Placement.physicalDistance(previousPlacements[previousPlacementIndex], nextPlacements[nextPlacementIndex])
                                        * Placement.timeDistance(chosenMatches[time].stepStart - chosenMatches[time - 1].stepStart))

                        // TODO take into consideration the last few

                    }.minBy { it.distance }!! // find the shortest path to current from any past, this should never be null
                }

            }

            paths.add(nextPaths)

        }

        val bestPath = paths.last().minBy { it.distance }?.route!!

        // the path with the shortest physicalDistance to the last placement

        (clusters.size until chosenMatches.size).forEach { time ->
            // replaces all the placements in the current placement with the best ones

            val newCluster = NoteCluster(
                    chosenMatches[time].notes.map { it.start }.min() ?: -timeStepStart,
                    chosenMatches[time].possiblePlacement[bestPath[time]],
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

    private class PatternMatchingState(val tuning: Tuning, val clusterStart: Int) : Serializable {

        private val notes = mutableListOf<Note>()
        private var possibleMatches = mutableListOf<PossibleMatch>()

        var isDead = false
        val matches: MutableList<PossibleMatch> = mutableListOf()

        fun add(note: Note) {

            if (notes.isEmpty()) {

                possibleMatches.addAll(patters.map { pattern ->
                    PossibleMatch(note.start, clusterStart, tuning, pattern, mutableListOf(note))
                })
                matches.add(PossibleMatch(note.start, clusterStart, tuning, null, mutableListOf(note)))

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
            notes.add(note)
        }

    }

    private data class PossibleMatch constructor(val stepStart: Int, val clusterStart: Int, private val tuning: Tuning, val pattern: ChordPattern?, val notes: MutableList<Note>) : Serializable {

        val isValid: Boolean
            get() = (pattern?.notes?.size ?: 1 >= tuning.size) && validRoots.isNotEmpty()
        val isPossible: Boolean
            get() = (pattern?.notes?.size ?: 1 >= tuning.size) && possibleRoots.isNotEmpty()

        private var placementCombinations: List<List<Int>> = listOf()
        private val possibleRoots: MutableList<Note> = mutableListOf()

        val validRoots: MutableList<Note> = mutableListOf()
        val possiblePlacement: List<List<Placement>>
            get() = placementCombinations.map { combination ->
                combination.mapIndexed { index, i -> tuning.getPlacements(notes[index])[i] }
            }

        init {
            val startNotes = notes.toList()
            notes.clear()
            startNotes.forEach { addNote(it) }
        }

        internal fun addNote(newNote: Note) {

            notes.add(newNote)
            val newPlacement = tuning.getPlacements(newNote.pitch)

            placementCombinations = if (notes.size == 1) {
                (0 until newPlacement.size).map { listOf(it) }
            } else {
                // this considers what combination you would get if you were to add each new possible combination to each existing one
                placementCombinations.flatMap { existing ->

                    (0 until newPlacement.size).map { new ->
                        existing + new
                    }.filter { listOfPlacementIndices ->
                                val placements = listOfPlacementIndices.mapIndexed { index, i ->
                                    tuning.getPlacements(notes[index])[i]
                                }
                                return@filter Placement.isPossible(placements, pattern)
                            }

                }
            }

            possibleRoots.add(newNote)
            possibleRoots.removeIf { !possibleWithRoot(it) }
            validRoots.clear()
            validRoots.addAll(possibleRoots.filter { validWithRoot(it) })
        }

        private fun possibleWithRoot(root: Note): Boolean {
            return placementCombinations.isNotEmpty() && (pattern == null || notes.all {
                (it.pitch - root.pitch).floorMod(12) in pattern.notes
            })
        }

        private fun validWithRoot(root: Note): Boolean {
            return placementCombinations.isNotEmpty() && (pattern == null || pattern.notes.none {
                (it + root.pitch) !in notes.map { it.pitch }
            })
        }

    }

    private data class Path(val route: List<Int>, val distance: Double) : Serializable

    data class ChordPattern(val title: String, val suffix: String, val notes: List<Int>, val maxStringSep: Int?) : Serializable {
        constructor(title: String, suffix: String, vararg notes: Int, maxStringSep: Int? = null)
                : this(title, suffix, notes.toList(), maxStringSep)
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
