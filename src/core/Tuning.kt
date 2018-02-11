package core

import core.Note.Companion.pitch
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
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

    /**
     * An easy to use constructor for testing the Tuning class
     * e.g. Tuning("E2","A3")
     */
    constructor(name: String, vararg strings: String, capo: Int = DEFAULT_CAPO, maxFret: Int = DEFAULT_MAX_FRET)
            : this(name, strings.mapNotNull { it.pitch }.reversed(), capo, maxFret)

    @Transient
    var samePlacements: Map<Int, List<Int>>
    @Transient
    var placements: List<Placement>

    init {

        placements = calculatePlacements()
        samePlacements = groupNotes(placements)

    }

    private fun calculatePlacements() = (0 until strings.size).flatMap { string ->
        (0..maxFret - capo).map { fret ->
            Placement(fret, string)
        }
    }

    private fun groupNotes(placements: List<Placement>) = placements.mapIndexed { index, _ -> index }
            .groupBy { strings[placements[it].string] + placements[it].fret + capo }


    internal fun findPlacements(pitch: Int): List<Int> = samePlacements.getOrElse(pitch) { listOf() }

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
     * Checks if a pitch has any samePlacements in this tuning
     */
    operator fun contains(pitch: Int): Boolean = findPlacements(pitch).isNotEmpty()

    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {
        output.defaultWriteObject()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        placements = calculatePlacements()
        samePlacements = groupNotes(placements)

    }

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