package core

data class Section(
        val sampleStart: Int,
        val timeStepStart: Int,
        val clusterStart: Int,
        val samples: MutableList<Float> = mutableListOf(),
        val timeSteps: MutableList<TimeStep> = mutableListOf(),
        val clusters: MutableList<NoteCluster> = mutableListOf(),
        var isGathered: Boolean = false,
        var isProcessed: Boolean = false
) {

    constructor(after: Section) : this(after.sampleEnd, after.timeStepEnd, after.clusterEnd) // new Section

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

    private val paths: MutableList<List<Path>> = mutableListOf()
    private val possiblePlacements: MutableList<List<Placement>> = mutableListOf()

    val chordController = PatternMatcher()

    /**
     * This class is used to store all the possible paths when placements are optimised
     * @property route The list of indices for all the possible placemetns
     * @property distance The distance for the particular path
     */
    private data class Path(val route: List<Int>, val distance: Double)

    fun addSamples(newSamples: FloatArray) {
        samples.addAll(newSamples.toTypedArray())
    }

    fun addTimeStep(timeStep: TimeStep) {
        timeSteps.add(timeStep)

//        timeStep.notes.filter { it.pitch in tuning } // the pitch must be within the playable range of the guitar
//                .filter { !notes.contains(it) } // if this note hasn't been added yet
//                .forEach {
//                    notes.add(it)
//                    possiblePlacements.add(Recording.findPlacements(it, tuning))
//                    chordController.feed(it)
//                }
//
//        optimiseForward(0) // re-optimise the placements
//        TODO make this more efficient

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

//        if (paths.size > 0) { // if there is a path
//            // TODO debug to find if this is necessary
//
//            val bestPath = paths[paths.size - 1].minBy { it.distance }?.route!!
//            // the path with the shortest distance to the last placement
//
//            (from until possiblePlacements.size).forEach {
//                // replaces all the placements in the current placement with the best ones
//                val currentPlacement = possiblePlacements[it][bestPath[it]]
//
//                if (it < placements.size) {
//                    placements[it] = currentPlacement
//                } else {
//                    placements.add(currentPlacement)
//                }
//            }
//
//        }

    }

    override fun toString(): String {
        return "Section(sampleStart=$sampleStart, timeStepStart=$timeStepStart, clusterStart=$clusterStart, samples.size=${samples.size}, timeSteps=$timeSteps, clusters=$clusters, paths=$paths, possiblePlacements=$possiblePlacements, chordController=$chordController)"
    }


    companion object {

        const val minStepLength = 10

    }

}
