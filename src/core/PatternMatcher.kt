package core

import kotlin.math.max

/**
 * This is the object which does all of the pattern matching and chord recognition
 * @author Kacper Lubisz
 * @property tuning the tuning the recording this is part of uses
 * @property clusters the mutable list of clusters that is also a property of the section this is acting on
 * @property notes the notes that the matcher has already seen
 * @property liveStates the list of pattern matching states that are still alive
 * @property chosenMatches the matches that have been chosen by the matcher
 * @property paths the 2d array of the placement paths taked by the best choices
 */
class PatternMatcher(private val tuning: Tuning, private val clusters: MutableList<NoteCluster>) {

    private val notes: MutableList<Note> = mutableListOf()
    private val liveStates: MutableList<PatternMatchingState> = mutableListOf()
    private val chosenMatches: MutableList<PossibleMatch> = mutableListOf()
    private val paths: MutableList<List<Path>> = mutableListOf()

    /**
     * Adds the note to the mather. This checks if the note has been added, and if not adds them individually
     * @param newNotes the notes that are being added at the new timeStep
     */
    internal fun feed(newNotes: List<Note>) {

        newNotes.filter { it.pitch in tuning } // the pitch must be within the playable range of the guitar
                .filter { it !in notes.takeLastWhile { it.start <= it.start } } // if this note hasn't been added yet
                .forEach {
                    notes.add(it)
                    feed(it)
                }

    }


    /**
     * This adds the note to the states that are still alive and then checks if those states came up with any matches.
     * The best match is then chosen and put into chosenMatches.  Then the states that can't produce new matches are
     * removed. A new state is created containing the the new note
     * @param note the new note beign added
     */
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
                                previousPaths[previousPlacementIndex].distance + distance
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

            val newCluster = NoteCluster(
                    chosenMatches[time].validRoots.minBy { it.start }?.pitch ?: 0,
                    chosenMatches[time].notes.minBy { it.start }?.start ?: 0,
                    chosenMatches[time].possiblePlacement[bestPath[time]],
                    chosenMatches[time].pattern
            )

            clusters.add(newCluster)
        }

    }

    /**
     * PatternMatchingStates are made each time a new note is added.
     * This means that the matcher considered any chord starting with any note
     * @property tuning the tuning used
     * @property clusterStart the start of the state in clusters
     */
    private class PatternMatchingState(val tuning: Tuning, val clusterStart: Int) {

        /* The notes the state has seen */
        private val notes = mutableListOf<Note>()
        /* The matches that haven't been excluded yet*/
        private var possibleMatches = mutableListOf<PossibleMatch>()

        /* Whether all of the matches have been excluded */
        var isDead = false
        /* The valid matches that have been found */
        val matches: MutableList<PossibleMatch> = mutableListOf()

        /**
         * Adds the note to all the possible matches and then removes the ones that won't make
         * matches and groups the ones that create valid placements
         * @param note the note being added
         */
        fun add(note: Note) {

            if (notes.isEmpty()) {

                possibleMatches.addAll(ChordPattern.values().map { pattern ->
                    PossibleMatch(note.start, clusterStart, tuning, pattern, mutableListOf(note))
                })
                matches.add(PossibleMatch(note.start, clusterStart, tuning, null, mutableListOf(note)))
                // create all the possible matches, one for each pattern and one for no pattern

            } else {

                if (notes.last().start + MAX_CHORD_SEPARATION < note.start) {
                    possibleMatches.clear()
                }

                possibleMatches.forEach { it.addNote(note) }
                possibleMatches.removeIf { !it.isPossible }
                // remove matches that are impossible

                possibleMatches.filter { it.isValid }.forEach { matches.add(it.copy()) }
                // add the possible ones to matches

                if (possibleMatches.isEmpty()) {
                    isDead = true
                }

            }
            notes.add(note)
        }

    }

    /**
     * This stores a possible match which evolves as new notes are added.
     * As notes are added to the match its validity and possibility changes, which changes if it gets eliminated or not in it's state
     * When a PatternMatchingState is made it starts with possible matches for each pattern which are then eliminated as new notes are added
     *
     * @property stepStart the relative step start
     * @property clusterStart the realtive cluster start
     * @property tuning the tuning used by the matcher/recording
     * @property pattern the pattern that this state is testing
     * @property notes the notes that this match already has
     */
    private data class PossibleMatch constructor(val stepStart: Int, val clusterStart: Int, private val tuning: Tuning, val pattern: ChordPattern?, val notes: MutableList<Note>) {

        val isValid: Boolean
            get() = (pattern?.notes?.size ?: 1 <= tuning.size) && validRoots.isNotEmpty()
        val isPossible: Boolean
            get() = (pattern?.notes?.size ?: 1 <= tuning.size) && possibleRoots.isNotEmpty()

        private var placementCombinations: List<List<Int>> = listOf()
        /* Similar to a path but stores the permutations of possible placements */
        private val possibleRoots: MutableList<Note> = mutableListOf()
        /* The list of roots that still could be possible with this pattern*/

        val validRoots: MutableList<Note> = mutableListOf()
        /* The roots that are valid and are matches at the current state of the match*/
        val possiblePlacement: List<List<Int>>
            get() = placementCombinations.map { combination ->
                combination.mapIndexed { index, i -> tuning.findPlacements(notes[index].pitch)[i] }
            }
        /* converts placementCombinations to lists of placement indices in terms of the tuning and not the notes */

        init {
            val startNotes = notes.toList()
            notes.clear()
            startNotes.forEach { addNote(it) }
        }

        /**
         * Adds a new note to the PossibleMatch and then updates if the match is valid
         * @param newNote the note beig added
         */
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

    /**
     * This object stores thr rout through the placements and the resultant distance
     * For example, possible placements may be = [[a, b],[c, d]], and a path may be [0,1]
     * This would mean that the path taken is placement [a, d]
     * @property route the placement choices for each chosen match
     * @property distance a rating of how bad a path is, this is what is minimised
     */
    private data class Path(val route: List<Int>, val distance: Double)

    companion object {

        const val MAX_CHORD_SEPARATION = 10 // timeSteps

        fun Int.floorMod(other: Int) = ((this % other) + other) % other

    }

}