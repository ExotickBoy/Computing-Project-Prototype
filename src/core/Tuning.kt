package core

import core.Note.Companion.pitch
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * This class is for storing the permutation of strings, capo and max fret that a guitar could have
 *
 * @property name The display name of the tuning
 * @property capo Which fret the capo is on, this is like an offset the string notes
 * @property maxFret The highest fret that the pattern matching will take into consideration when using this tuning
 *
 */
data class Tuning(val name: String, val strings: List<Int>, val capo: Int = DEFAULT_CAPO, val maxFret: Int = DEFAULT_MAX_FRET) : Serializable {

    /**
     * An easy to use constructor for testing the Tuning class
     * e.g. Tuning("E2","A3")
     */
    constructor(name: String, vararg strings: String, capo: Int = DEFAULT_CAPO, maxFret: Int = DEFAULT_MAX_FRET)
            : this(name, strings.mapNotNull { it.pitch }.reversed(), capo, maxFret)

    @Transient // transient means that it won't be written to file
    var samePlacements: Map<Int, List<Int>>
    @Transient
    var placements: List<Placement>

    init {

        placements = calculatePlacements()
        samePlacements = groupNotes(placements)

    }

    /**
     * Finds all the placements possible with the current tuning
     * @return all the possible placements
     */
    private fun calculatePlacements() = (0 until strings.size).flatMap { string ->
        (0..maxFret - capo).map { fret ->
            Placement(fret, string)
        }
    }

    /**
     * This finds all the possible groups all the placements it is fed into what pitch they are
     * @return a map of pitch to list of possible placement
     */
    private fun groupNotes(placements: List<Placement>) = placements.mapIndexed { index, _ -> index }
            .groupBy { strings[placements[it].string] + placements[it].fret + capo }


    /**
     * This method finds the indices of the placements that a particular pitch can be played on
     * @return The list of indices
     */
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

    /**
     * This is an override method which is called when a tuning is being written to file
     */
    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {
        output.defaultWriteObject()
    }

    /**
     * This is an override method which is called when a tuning is being read from file
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        placements = calculatePlacements()
        samePlacements = groupNotes(placements)

    }

    companion object {

        // the standard tunings that are most likely to be used
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