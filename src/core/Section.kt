package core

data class Section(
        val recording: Recording,
        val sampleStart: Int,
        val timeStepStart: Int,
        val clusterStart: Int,
        val samples: MutableList<Float> = mutableListOf(),
        val timeSteps: MutableList<TimeStep> = mutableListOf(),
        val clusters: MutableList<NoteCluster> = mutableListOf(),
        var isGathered: Boolean = false,
        var isProcessed: Boolean = false
) {

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
    private val patternMatcher = PatternMatcher(recording.tuning, clusters)

    fun addSamples(newSamples: FloatArray) {
        samples.addAll(newSamples.toTypedArray())
    }

    fun addTimeStep(timeStep: TimeStep) {
        timeSteps.add(timeStep)

        timeStep.notes.filter { it.pitch in recording.tuning } // the pitch must be within the playable range of the guitar
                .filter { !notes.contains(it) } // if this note hasn't been added yet
                .forEach {
                    notes.add(it)
                    patternMatcher.feed(it)
                }

    }

    companion object {

        const val MIN_STEP_LENGTH = 10

    }

}
