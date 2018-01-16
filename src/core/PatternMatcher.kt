package core

import core.PatternMatcher.PossibleMatch.Companion.MAX_CHORD_LENGTH
import core.Placement.Companion.distance
import core.Placement.Companion.internalDistance
import java.io.Serializable

class PatternMatcher(val section: Section, val tuning: Tuning) : Serializable {

    private val states: MutableList<PatternMatchingState> = mutableListOf()
    private val liveStates: MutableList<PatternMatchingState> = mutableListOf()

    private val paths: MutableList<List<Path>> = mutableListOf()
    private val possiblePlacements: MutableList<List<List<Placement>>> = mutableListOf()

    private val chosenMatches: MutableList<PossibleMatch> = mutableListOf()

    fun feed(note: Note) {

        println("\nFED")

        val newState = PatternMatchingState(section, tuning, states.size)
        states.add(newState)
        liveStates.add(newState)

        liveStates.forEach { it.add(note) }
        liveStates.removeIf { it.isDead }

        val new = liveStates.minBy { it.clusterStart }!!

        val removeLast = when {
            chosenMatches.isEmpty() -> {
                chosenMatches.add(new.matches.last())
                possiblePlacements.add(chosenMatches[chosenMatches.lastIndex].possiblePlacements)
                false
            }
            chosenMatches[chosenMatches.lastIndex].clusterStart >= new.clusterStart -> {
                chosenMatches[chosenMatches.lastIndex] = new.matches.last()
                possiblePlacements[possiblePlacements.lastIndex] = chosenMatches[chosenMatches.lastIndex].possiblePlacements
                true
            }
            else -> {
                chosenMatches.add(new.matches.last())
                possiblePlacements.add(chosenMatches[chosenMatches.lastIndex].possiblePlacements)
                false
            }
        }

        println("ADDING NEW MATCH ${new.matches.last()}")
        println("REPLACING LAST = $removeLast")

        if (removeLast) {
            paths.removeAt(paths.lastIndex)
            section.clusters.removeAt(section.clusters.lastIndex)
        }

        (paths.size until possiblePlacements.size).forEach { time ->

            val currentPlacements = possiblePlacements[time]

            val nextPaths = if (time == 0) { // no previous paths

                (0 until currentPlacements.size).map {
                    Path(listOf(it), internalDistance(currentPlacements[it]))
                    // start each path with the starting distance to the placement
                }

            } else {

                val previousPaths = paths[time - 1]

                (0 until currentPlacements.size).map { to ->
                    (0 until previousPaths.size).map { from ->
                        // for each possible pair of the placements in the last time and the current one

                        Path(previousPaths[from].route + to,
                                previousPaths[from].route.mapIndexed { index, place ->
                                    distance(possiblePlacements[index][place], currentPlacements[to])
                                }.takeLast(10).sum())

                    }.minBy { it.distance }!! // find the shortest path to current from any past
                }

            }

            paths.add(nextPaths)

        }

        val bestPath = paths.last().minBy { it.distance }?.route!!
        // the path with the shortest distance to the last placement

        (section.clusters.size until chosenMatches.size).forEach { time ->
            // replaces all the placements in the current placement with the best ones

            val newCluster = NoteCluster(
                    chosenMatches[time].notes.map { it.start }.min() ?: 0,
                    possiblePlacements[time][bestPath[time]],
                    chosenMatches[time].pattern?.title ?: ""
            )

            section.clusters.add(newCluster)
        }

    }

    private class PatternMatchingState(val section: Section, val tuning: Tuning, val clusterStart: Int) : Serializable {

        val notes = mutableListOf<Note>()

        var possibleMatches = mutableListOf<PossibleMatch>()
        var isDead = false

        val matches: MutableList<PossibleMatch> = mutableListOf()

        fun add(note: Note) {

            if (notes.isEmpty()) {

                possibleMatches.addAll(PossibleMatch.patters.map { pattern ->
                    PossibleMatch(note.start - section.timeStepStart, clusterStart, tuning, pattern, mutableListOf(note))
                })
                matches.add(PossibleMatch(note.start - section.timeStepStart, clusterStart, tuning, note))

            } else {

                if (notes[0].start + MAX_CHORD_LENGTH < note.start) {
                    possibleMatches.clear()
                }

                possibleMatches.forEach { it.addNote(note) }
                possibleMatches.removeIf { !it.isPossible }

                val newMatch = possibleMatches.find { it.isValid }
                if (newMatch != null) {
                    matches.add(newMatch)
                }

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

    private data class PossibleMatch(val stepStart: Int, val clusterStart: Int, private val tuning: Tuning, val pattern: ChordPattern?, val notes: MutableList<Note>) {

        constructor(stepStart: Int, clusterStart: Int, tuning: Tuning, note: Note) : this(stepStart, clusterStart, tuning, null, mutableListOf()) {
            addNote(note)
        }

        val isValid: Boolean
            get() = validRoots.isNotEmpty()
        val isPossible: Boolean
            get() = possibleRoots.isNotEmpty()

        var possiblePlacements: List<List<Placement>> = mutableListOf()

        private val possibleRoots: MutableList<Note> = mutableListOf()
        private val validRoots: MutableList<Note> = mutableListOf()

        internal fun addNote(newNote: Note) {

            if (newNote.pitch !in notes.map { it.pitch }) {

                notes.add(newNote)

                possiblePlacements = findPossiblePlacements()
                if (possiblePlacements.isEmpty()) {
                    possibleRoots.clear()
                }

                possibleRoots.add(newNote)
                possibleRoots.removeIf { !possibleWithRoot(it) }
                validRoots.clear()
                validRoots.addAll(possibleRoots.filter { validWithRoot(it) })
            }

        }

        private fun findPossiblePlacements(): List<List<Placement>> {

            val notePlacements: List<List<Placement>> = notes.map { findPossiblePlacements(it, tuning) }
            val possiblePlacementPaths: MutableList<PlacementPath> = mutableListOf()

            notes.forEachIndexed { index, _ ->
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

            return possiblePlacementPaths.map { it.placements }

        }

        private fun possibleWithRoot(root: Note): Boolean {
            return pattern == null || notes.all {
                (it.pitch - root.pitch).floorMod(12) in pattern.notes
            }
        }

        private fun validWithRoot(root: Note): Boolean {
            return pattern == null || pattern.notes.none {
                (it + root.pitch) !in notes.map { it.pitch }
            }
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

        companion object {

            internal val patters = listOf(
                    ChordPattern("Major", "M", 0, 4, 7),
                    ChordPattern("Minor", "m", 0, 3, 7),
                    ChordPattern("Diminished", "d", 0, 3, 6),
                    ChordPattern("Augmented", "a", 0, 4, 8),
                    ChordPattern("+7", "7", 0, 7),
                    ChordPattern("+5", "5", 0, 5)
            )

            const val MAX_CHORD_LENGTH = 30 // timeSteps

            fun Int.floorMod(other: Int) = ((this % other) + other) % other

        }

        data class PlacementPath(val placements: List<Placement>)

        data class ChordPattern(val title: String, val suffix: String, val notes: List<Int>) {
            constructor(title: String, suffix: String, vararg notes: Int) : this(title, suffix, notes.asList())
        }


    }

    /**
     * This class is used to store all the possible paths when placements are optimised
     * @property route The list of indices for all the possible placemetns
     * @property distance The distance for the particular path
     */
    private data class Path(val route: List<Int>, val distance: Double)

}
