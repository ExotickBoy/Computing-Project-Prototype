package core

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * This class is for storing one permutation of how a pitch can be played on a tuning
 * @author Kacper Lubisz
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

    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {
        output.defaultWriteObject()
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
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

        fun internalDistance(placements: List<Placement>): Double {

            return INTERNAL_SCALING_FACTOR * (placements.map { it.startDistance() }.average() +
                    euclideanNorm(
                            placements.map { it.fret }.range(),
                            placements.map { it.string }.range()
                    ) * placements.size)
        }

        fun physicalDistance(firsts: List<Placement>, seconds: List<Placement>): Double {

            return firsts.flatMap { first ->
                seconds.map { second ->
                    physicalDistance(first, second)
                }
            }.average()

        }

        private fun physicalDistance(a: Placement, b: Placement): Double {

            return if ((a.string - b.string) in -1..1 && (a.fret == 0 || b.fret == 0)) 0.0
            else {
                val fretRange = abs(a.fret - b.fret) * FRET_SCALING_FACTOR
                val stringRange = abs(a.fret - b.fret)
                euclideanNorm(fretRange, stringRange)
            }
            // this is because a placement on the 0th fret is the open string, which can be played from anywhere

        }

        /**
         * This function lets you find how significant something is through time.
         * It means that the location of something far away isn't significant
         * @param time the time between the steps
         * @return a function representing the significance of this time
         */
        private fun timeDistance(time: Int): Double = TIME_FACTOR_BASE.pow(-time)

        fun timeDistance(a: Int, b: Int): Double = timeDistance(max(a, b) - min(a, b))

        /**
         * The Euclidean normal returns the physicalDistance in euclidean space given the difference in each dimension.
         * e.g. (3,4) -> √(3²+4²) = 5
         * @param dims the variable argument list of distances in each dimension
         * @return the euclidean physicalDistance
         */
        private fun euclideanNorm(vararg dims: Number): Double = Math.sqrt(dims.map { it.toDouble() }.map { it.pow(2) }.sum())

        private fun List<kotlin.Int>.range(): kotlin.Int {
            val from = this.min()
            val to = this.max()
            return if (from != null && to != null) to - from else 0

        }

    }

}
