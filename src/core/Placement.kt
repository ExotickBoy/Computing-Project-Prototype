package core

import java.lang.Math.exp
import kotlin.math.pow

/**
 * This class is for storing one permutation of how a pitch can be played on a tuning
 * @author Kacper Lubisz
 * @property fret the fret that the note is played on
 * @property string the string that the note is played on
 * @property time the start at which the note is played
 */
data class Placement(val fret: Int, val string: Int, val note: Note) {

    /**
     * This method finds the distance between two placements.
     * The function should be high if the distance between the two points on the guitar is high (i.e. fret, string) and
     * low when there is a lot of time between the two placements (giving the player time to move their hand)
     * @param other the other placement that should be compared
     */
    infix fun distance(other: Placement): Double {

        return if (string == other.string && other.note.start < note.end) {
            // this is because two notes can't be played on the same string at the same time
            Double.MAX_VALUE
        } else {

            euclideanNorm(fretDistance(this.fret, other.fret), string - other.string) *
                    timeDistance(note.end - other.note.start)


        }

    }

    /**
     * This function finds the distance to a placement if it is the first one
     */
    fun startDistance(): Double {
        return fret * FRET_SCALING_FACTOR
        // high frets are punished for the purpose of encouraging the placements to be lower on the guitar
    }

    companion object {

        /**
         * The scaling factor for the distance function, used for optimising the distance function
         */
        private const val FRET_SCALING_FACTOR: Double = 2.5

        /**
         * The base in the exponential function relating to time's significance
         */
        private const val TIME_FACTOR_BASE = 1.08

        /**
         * The importance of the distance between the frets
         * @param a the first fret
         * @param b the second fret
         * @return the distance between the frets
         */
        private fun fretDistance(a: Int, b: Int): Double {

            return if (a == 0 || b == 0) 0.0
            else 1 - exp(-a * Math.pow(a - b.toDouble(), 2.0))
            // this is because a placement on the 0th fret is the open string, which can be played from anywhere

        }

        /**
         * This function lets you find how significant something is through time.
         * It means that the location of something far away isn't significant
         * @param time the time between the steps
         * @return a function representing the significance of this time
         */
        private fun timeDistance(time: Int): Double = TIME_FACTOR_BASE.pow(time)

        /**
         * The Euclidean normal returns the distance in euclidean space given the difference in each dimension.
         * e.g. (3,4) -> √(3²+4²) = 5
         * @param dims the variable argument list of distances in each dimension
         * @return the euclidean distance
         */
        private fun euclideanNorm(vararg dims: Number): Double = Math.sqrt(dims.map { it.toDouble() }.map { it * it }.sum())

    }

}