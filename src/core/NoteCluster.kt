package core

import java.io.Serializable


data class NoteCluster(val relTimeStepStart: Int, val placements: List<Placement>, val heading: String, val boldHeading: Boolean) : Serializable