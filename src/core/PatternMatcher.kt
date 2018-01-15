package core

import core.PatternMatcher.PossibleMatch.Companion.MAX_CHORD_LENGTH
import java.io.Serializable

class PatternMatcher(val tuning: Tuning, val clusters: MutableList<NoteCluster>) : Serializable {

    private val states: MutableList<PatternMatchingState> = mutableListOf()
    private val liveStates: MutableList<PatternMatchingState> = mutableListOf()

    private val paths: MutableList<List<Path>> = mutableListOf()
    private val possiblePlacements: MutableList<List<Placement>> = mutableListOf()

    fun feed(note: Note) {

        val newState = PatternMatchingState(tuning, states.size)
        states.add(newState)
        liveStates.add(newState)

        val newDeadStates = mutableListOf<PatternMatchingState>()
        liveStates.forEach { it.add(note) }
        liveStates.removeIf {
            if (it.isDead) {
                newDeadStates.add(it)
            }
            return@removeIf it.isDead
        }

        println(states.map { it.matches })
        // optimise placements of new matches

    }


    private class PatternMatchingState(val tuning: Tuning, val start: Int) : Serializable {

        val notes = mutableListOf<Note>()

        var possibleMatches = mutableListOf<PossibleMatch>()
        var isDead = false

        val matches: MutableList<PossibleMatch> = mutableListOf()

        fun add(note: Note) {

            if (notes.isEmpty()) {

                possibleMatches.addAll(PossibleMatch.patters.map { pattern ->
                    PossibleMatch(tuning, pattern, mutableListOf(note))
                })

            } else {

                if (notes[0].start + MAX_CHORD_LENGTH < note.start) {
                    possibleMatches.clear()
                }

                possibleMatches.forEach { it.addNote(note) }
                possibleMatches.removeIf { !it.isPossible }

                matches.addAll(possibleMatches.filter { it.isValid }.map { it.copy() })

                if (possibleMatches.isEmpty()) {
                    isDead = true
                }

            }
            notes.add(note)
        }

        companion object {

            internal infix fun Int.floorMod(other: Int) = ((this % other) + other) % other

        }

    }

    private data class PossibleMatch(private val tuning: Tuning, private val pattern: ChordPattern, private val notes: MutableList<Note>) {

        val isValid: Boolean
            get() = validRoots.isNotEmpty()
        val isPossible: Boolean
            get() = possibleRoots.isNotEmpty()

        var possiblePlacements: List<List<Placement>> = mutableListOf()

        private val possibleRoots: MutableList<Note> = mutableListOf()
        private val validRoots: MutableList<Note> = mutableListOf()

        internal fun addNote(note: Note) {
            notes.add(note)

            val notePlacements: List<List<Placement>> = notes.map { findPlacements(it, tuning) }
            val possiblePlacementPaths: MutableList<PlacementPath> = mutableListOf()
            notes.forEachIndexed { index, note ->
                if (index == 0) {
                    possiblePlacementPaths.addAll(notePlacements[0].map { PlacementPath(listOf(it)) })
                } else {
                    val lastPossible = possiblePlacementPaths.toList()
                    possiblePlacementPaths.clear()
                    possiblePlacementPaths.addAll(lastPossible.flatMap {
                        notePlacements[index].map { next ->
                            PlacementPath(it.placements + next)
                        }
                    }.filter {
                        it.placements.map { it.string }.distinct().size == it.placements.size
                    })
                }
            } // this is essentially a breadth first search through this graph
            possiblePlacements = possiblePlacementPaths.map { it.placements }

            if (possiblePlacements.isEmpty()) {
                possibleRoots.clear()
            }

            possibleRoots.add(note)
            possibleRoots.removeIf { !possibleWithRoot(it) }
            validRoots.clear()
            validRoots.addAll(possibleRoots.filter { validWithRoot(it) })

        }

        private fun possibleWithRoot(root: Note): Boolean {
            return notes.all {
                (it.pitch - root.pitch).floorMod(12) in pattern.notes
            }
        }

        private fun validWithRoot(root: Note): Boolean {
            return pattern.notes.none {
                (it + root.pitch) !in notes.map { it.pitch }
            }
        }

        /**
         * Finds all the placements of a note in a tuning
         * @param note The note to be found for
         * @param tuning The tuning to be searched
         * @return All the possible placements for a tuning
         */
        private fun findPlacements(note: Note, tuning: Tuning): List<Placement> {

            return tuning.strings.mapIndexed { index, it ->
                Placement(tuning, note.pitch - it, index, note)
            }.filter {
                it.fret in tuning.capo..tuning.maxFret
            }

        }

        companion object {

            internal val patters = listOf(
                    ChordPattern("Major", "M", 0, 4, 7),
                    ChordPattern("Minor", "m", 0, 3, 7),
                    ChordPattern("Diminished", "d", 0, 3, 6),
                    ChordPattern("Augmented", "a", 0, 4, 8),
                    ChordPattern("+7", "7", 0, 7),
                    ChordPattern("+5", "5", 0, 5)
            )

            const val MAX_CHORD_LENGTH = 15 // timeSteps

            fun Int.floorMod(other: Int) = ((this % other) + other) % other

        }

        data private class PlacementPath(val placements: List<Placement>)

        data private class ChordPattern(val title: String, val suffix: String, val notes: List<Int>) {
            constructor(title: String, suffix: String, vararg notes: Int) : this(title, suffix, notes.asList())
        }


    }

    /**
     * This optimised the possible placements from a particular placement forward
     * @param from The time at which the optimisation should start
     */
//    private fun optimiseForward(from: Int) {
//
//        for (time in from until possiblePlacements.size) {
//
//            val currentPlacements = possiblePlacements[time]
//
//            val nextPaths = if (time == 0) { // no previous paths
//
//                (0 until currentPlacements.size).map {
//                    Section.Path(listOf(it), currentPlacements[it].startDistance())
//                    // start each path with the starting distance to the placement
//                }
//
//            } else {
//
//                val previousPaths = paths[time - 1]
//
//                (0 until currentPlacements.size).map { current ->
//                    (0 until previousPaths.size).map { past ->
//                        // for each possible pair of the placements in the last time and the current one
//
//                        Section.Path(previousPaths[past].route + current,
//                                previousPaths[past].route.mapIndexed { index, place ->
//                                    possiblePlacements[index][place] distance currentPlacements[current]
//                                }.takeLast(10).sum())
//
//                    }.minBy { it.distance }!! // find the shortest path to current from any past
//                }
//
//            }
//
//            if (time < paths.size) { // if this path already exists and needs to be replaced
//                paths[time] = nextPaths
//            } else {
//                paths.add(nextPaths)
//            }
//
//        }
//
////        if (paths.size > 0) { // if there is a path
////            // TODO debug to find if this is necessary
////
////            val bestPath = paths[paths.size - 1].minBy { it.distance }?.route!!
////            // the path with the shortest distance to the last placement
////
////            (from until possiblePlacements.size).forEach {
////                // replaces all the placements in the current placement with the best ones
////                val currentPlacement = possiblePlacements[it][bestPath[it]]
////
////                if (it < placements.size) {
////                    placements[it] = currentPlacement
////                } else {
////                    placements.add(currentPlacement)
////                }
////            }
////
////        }
//
//    }

    /**
     * This class is used to store all the possible paths when placements are optimised
     * @property route The list of indices for all the possible placemetns
     * @property distance The distance for the particular path
     */
    private data class Path(val route: List<Int>, val distance: Double)

}
