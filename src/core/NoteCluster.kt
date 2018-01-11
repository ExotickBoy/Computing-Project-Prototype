package core

abstract class NoteCluster(val relTimeStepStart: Int) {
    abstract val placements: List<Placement>
    abstract val heading: String
}