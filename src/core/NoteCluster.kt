package core

import core.Note.Companion.noteLetterShort
import java.io.Serializable


/**
 * This data class represents a played note or cluster of notes.
 * For example, a single played note is a cluster, but also a played chord is a cluster
 *
 * @author Kacper Lubisz
 *
 * @property root the root note of cluster if it is a chord, else just the note
 * @property relTimeStepStart the relative start of the cluster in tim steps
 * @property placements the indices of placements which make up this cluster
 * @property pattern the chord which this cluster represents, null means single note
 */
data class NoteCluster(private val root: Int, val relTimeStepStart: Int, val placements: List<Int>, val pattern: ChordPattern?) : Serializable {

    /* The heading that is shown above this cluster in the NoteOutputView */
    val heading: String
        get() = root.noteLetterShort + " " + (pattern?.suffix ?: "")

}