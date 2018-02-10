package core

import core.Note.Companion.pitch
import java.io.Serializable

/**
 * This class stores the permutation of strings that a guitar could have
 *
 * @author Kacper Lubisz
 *
 * @property strings The list of strings that the tuning has
 * @property capo The placement of the capo in the guitar, (this also acts as a minimum fret and should be displayed as 0)
 * @property maxFret The highest fret that the tuning has
 */
data class Tuning(val name: String, val strings: List<Int>, val capo: Int = DEFAULT_CAPO, val maxFret: Int = DEFAULT_MAX_FRET) : Serializable {

    constructor(name: String, vararg strings: String, capo: Int = DEFAULT_CAPO, maxFret: Int = DEFAULT_MAX_FRET)
            : this(name, strings.mapNotNull { it.pitch }.reversed(), capo, maxFret)
    // an easy to use constructor for testing the Tuning class
    // e.g. Tuning("E2","A3")

    private val placements: MutableMap<Int, List<Placement>> = mutableMapOf()

    internal fun getPlacements(pitch: Int): List<Placement> = placements.getOrPut(pitch) { findPlacements(pitch) }

    private fun findPlacements(pitch: Int): List<Placement> = strings.mapIndexedNotNull { index, it ->
        val placement = Placement(pitch - it - capo, index)
        if (placement.fret in 0..(maxFret - capo)) placement else null
    }

    fun getPlacements(pitch: Note): List<Placement> = getPlacements(pitch.pitch)

    /**
     * Returns the indexed string of the tuning
     * @param index the index of a certain string
     */
    operator fun get(index: Int) = strings[index]

    /**
     * The size of the tuning is the same as the number of strings that it has
     * @return the number of strings
     */
    val size: Int
        get() = strings.size

    /**
     * Checks if a pitch has any placements in this tuning
     */
    operator fun contains(pitch: Int): Boolean = findPlacements(pitch).isNotEmpty()

    companion object {

        val DEFAULT_TUNINGS: List<Tuning> = mutableListOf(
                Tuning("Standard Guitar", "E2", "A2", "D3", "G3", "B3", "E4"),
                Tuning("Standard Bass", "E1", "A1", "D2", "G2")
        )

        const val DEFAULT_NAME: String = "Tuning"
        const val DEFAULT_CAPO = 0
        const val DEFAULT_MAX_FRET = 15

        const val MAX_MAX_FRET = 20

    }

}