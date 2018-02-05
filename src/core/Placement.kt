package core

import java.io.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * This class is for storing one permutation of how a pitch can be played on a tuning
 *
 * @author Kacper Lubisz
 *
 * @property fret the fret that the note is played on
 * @property string the string that the note is played on
 */
data class Placement(val fret: Int, val string: Int) : Serializable {

    /**
     * This function finds the physicalDistance to a placement if it is the first one
     */
    internal fun startDistance(): Double {
        return fret * FRET_SCALING_FACTOR
        // high frets are punished for the purpose of encouraging the placements to be lower on the guitar
    }

    companion object {

        /**
         * The scaling factor for the physicalDistance function, used for optimising the physicalDistance function
         */
        private const val FRET_SCALING_FACTOR: Double = 1.0
        private const val INTERNAL_SCALING_FACTOR: Double = 1.0
        private const val MAX_FRET_SEPARATION = 5

        /**
         * The base in the exponential function relating to time's significance
         */
        private const val TIME_FACTOR_BASE = 1.04

        /**
         * Finds if a list of placements together are possible under a certain pattern
         * @param placements the placements that will be tested
         * @param pattern the pattern the placements will be tested against
         * @return if it is possible
         */
        fun isPossible(placements: List<Placement>, pattern: ChordPattern?): Boolean {

            val strings = placements.map { it.string }
            val stringSpan = strings.range() + 1
            val fretSpan = placements.map { it.fret }.range() + 1
            return when {
                strings.distinct().size != strings.size -> false
                pattern?.maxStringSpan != null && stringSpan > pattern.maxStringSpan -> false
                fretSpan > MAX_FRET_SEPARATION -> false
                else -> true
            }

        }

        /**
         * The heuristic cost function of a placement.
         * This function means that chords which has placements close together are more likely to be chosen
         * @param placements the placements in question
         * @return the cost of those placements
         */
        fun internalDistance(placements: List<Placement>): Double {

            return INTERNAL_SCALING_FACTOR * (placements.map { it.startDistance() }.average() +
                    euclideanNorm(
                            placements.map { it.fret }.range(),
                            placements.map { it.string }.range()
                    ) * placements.size)

        }

        /**
         * The physical distance heuristic between two groups of placements
         * @param firsts the first group of placements
         * @param firsts the first group of placements
         * @return the cost of the distance between the groups
         */
        fun physicalDistance(firsts: List<Placement>, seconds: List<Placement>): Double {

            return firsts.flatMap { first ->
                seconds.map { second ->
                    physicalDistance(first, second)
                }
            }.average()

        }

        /**
         * The function of heuristic distance between two singular placements (not groups)
         * @param a the first placement
         * @param b the second placement
         * @return the distance between the two placements
         */
        private fun physicalDistance(a: Placement, b: Placement): Double {

            // this is because a placement on the 0th fret is the open string, which can be played from anywhere
            return if ((a.string - b.string) in -1..1 && (a.fret == 0 || b.fret == 0)) 0.0
            else {
                val fretRange = abs(a.fret - b.fret) * FRET_SCALING_FACTOR
                val stringRange = abs(a.fret - b.fret)
                euclideanNorm(fretRange, stringRange)
            }

        }

        /**
         * This function lets you find how significant something is through time.
         * It means that the location of something far away isn't significant
         * @param time the time between the steps
         * @return a function representing the significance of this time
         */
        private fun timeDistance(time: Int): Double = TIME_FACTOR_BASE.pow(-time)

        /**
         * The heuristic modifier for physical distance based on time distance
         */
        fun timeDistance(a: Int, b: Int): Double = timeDistance(max(a, b) - min(a, b))

        /**
         * The Euclidean normal returns the physicalDistance in euclidean space given the difference in each dimension.
         * e.g. (3,4) -> √(3²+4²) = 5
         * @param dims the variable argument list of distances in each dimension
         * @return the euclidean physicalDistance
         */
        private fun euclideanNorm(vararg dims: Number): Double = Math.sqrt(dims.map { it.toDouble() }.map { it.pow(2) }.sum())

        /**
         * This extension function finds that range of a list of integers
         */
        private fun List<kotlin.Int>.range(): kotlin.Int {
            val from = this.min()
            val to = this.max()
            return if (from != null && to != null) to - from else 0

        }

    }

}
