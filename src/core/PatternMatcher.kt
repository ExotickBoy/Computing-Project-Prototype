package core

import java.io.Serializable
import kotlin.math.max

// not serializable
class PatternMatcher(private val tuning: Tuning, private val clusters: MutableList<NoteCluster>) {

    private val notes: MutableList<Note> = mutableListOf()
    private val liveStates: MutableList<PatternMatchingState> = mutableListOf()
    private val chosenMatches: MutableList<PossibleMatch> = mutableListOf()
    private val paths: MutableList<List<Path>> = mutableListOf()

    internal fun feed(newNotes: List<Note>) {

        newNotes.filter { it.pitch in tuning } // the pitch must be within the playable range of the guitar
                .filter { it !in notes.takeLastWhile { it.start <= it.start } } // if this note hasn't been added yet
                .forEach {
                    notes.add(it)
                    feed(it)
                }

    }

    private fun feed(note: Note) {

        val newState = PatternMatchingState(tuning, chosenMatches.size)
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
                    Path(listOf(it), Placement.internalDistance(nextPlacements[it].map { tuning.placements[it] }))
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

                        val currentRoot = previousPaths[previousPlacementIndex].route

                        val distance = (max(0, time - 3)..(time - 1)).map { otherIndex ->

                            Placement.physicalDistance(
                                    chosenMatches[otherIndex].possiblePlacement[currentRoot[otherIndex]].map { tuning.placements[it] },
                                    nextPlacements[nextPlacementIndex].map { tuning.placements[it] }
                            ) * Placement.timeDistance(
                                    chosenMatches[time].stepStart,
                                    chosenMatches[otherIndex].stepStart
                            )

                        }.sum()

                        Path(previousPaths[previousPlacementIndex].route + nextPlacementIndex,
                                distance
                        )

                    }.minBy { it.distance }!! // find the shortest path to current from any past, this should never be null
                }

            }

            paths.add(nextPaths)

        }

        val bestPath = paths.last().minBy { it.distance }?.route!!

        // the path with the shortest physicalDistance to the last placement

        (clusters.size until chosenMatches.size).forEach { time ->
            // replaces all the placements in the current placement with the best ones

            val leadNote = chosenMatches[time].notes.minBy { it.start }

            val newCluster = NoteCluster(
                    leadNote?.pitch ?: 0,
                    leadNote?.start ?: 0,
                    chosenMatches[time].possiblePlacement[bestPath[time]],
                    chosenMatches[time].pattern
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

                possibleMatches.addAll(ChordPattern.values().map { pattern ->
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
            get() = (pattern?.notes?.size ?: 1 <= tuning.size) && validRoots.isNotEmpty()
        val isPossible: Boolean
            get() = (pattern?.notes?.size ?: 1 <= tuning.size) && possibleRoots.isNotEmpty()

        private var placementCombinations: List<List<Int>> = listOf()
        private val possibleRoots: MutableList<Note> = mutableListOf()

        val validRoots: MutableList<Note> = mutableListOf()
        val possiblePlacement: List<List<Int>>
            get() = placementCombinations.map { combination ->
                combination.mapIndexed { index, i -> tuning.findPlacements(notes[index].pitch)[i] }
            }

        init {
            val startNotes = notes.toList()
            notes.clear()
            startNotes.forEach { addNote(it) }
        }

        internal fun addNote(newNote: Note) {

            notes.add(newNote)
            val newPlacement = tuning.findPlacements(newNote.pitch)

            placementCombinations = if (notes.size == 1) {
                (0 until newPlacement.size).map { listOf(it) }
            } else {
                // this considers what combination you would get if you were to add each new possible combination to each existing one
                placementCombinations.flatMap { existing ->

                    (0 until newPlacement.size).map { new ->
                        existing + new
                    }.filter { listOfPlacementIndices ->
                                val placements = listOfPlacementIndices.mapIndexed { index, i ->
                                    tuning.findPlacements(notes[index].pitch)[i]
                                }
                                return@filter Placement.isPossible(placements.map { tuning.placements[it] }, pattern)
                            }

                }
            }

//            if (notes.size == 2 && notes.distinctBy { it.pitch }.size == notes.size && pattern == ChordPattern.SIMPLE_POWER_CHORD) {
//                println("bam, this is the hot case")
//            } TODO get rid of this after debugging is over

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
            val pitches = notes.map { it.pitch - root.pitch }
            return placementCombinations.isNotEmpty() && (pattern == null || pattern.notes.none {
                it !in pitches
            })
        }

    }

    private data class Path(val route: List<Int>, val distance: Double) : Serializable

    companion object {

        const val MAX_CHORD_SEPARATION = 10 // timeSteps

        fun Int.floorMod(other: Int) = ((this % other) + other) % other

    }

}