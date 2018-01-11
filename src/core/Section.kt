package core

// TODO
data class Section(
        val sampleStart: Int,
        val timeStepStart: Int,
        val clusterStart: Int,
        val samples: MutableList<Float> = mutableListOf(),
        val timeSteps: MutableList<TimeStep> = mutableListOf(),
        val noteClusters: MutableList<NoteCluster> = mutableListOf()
) {

    constructor(after: Section) : this(after.sampleEnd, after.timeStepEnd, after.clusterEnd) // new Section

    private val paths: MutableList<List<Path>> = mutableListOf()
    private val possiblePlacements: MutableList<List<Placement>> = mutableListOf()

    val timeStepEnd
        get() = timeStepStart + timeSteps.size

    val sampleEnd
        get() = sampleStart + samples.size

    val clusterEnd
        get() = clusterStart + noteClusters.size

    val timeStepRange
        get() = timeStepStart until timeStepEnd

    val sampleRange
        get() = sampleStart until sampleEnd

    val clusterRange
        get() = clusterStart until clusterEnd


    /**
     * This class is used to store all the possible paths when placements are optimised
     * @property route The list of indices for all the possible placemetns
     * @property distance The distance for the particular path
     */
    private data class Path(val route: List<Int>, val distance: Double)

    companion object {

        const val minStepLength = 10

    }

}
