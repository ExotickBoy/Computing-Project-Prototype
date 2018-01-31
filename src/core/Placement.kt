package core

import java.io.Serializable
import kotlin.math.abs
import kotlin.math.pow

/**
 * This class is for storing one permutation of how a pitch can be played on a tuning
 * @author Kacper Lubisz
 * @property fret the fret that the note is played on
 * @property string the string that the note is played on
 * @property time the start at which the note is played
 */
data class Placement(val tuning: Tuning, val fret: Int, val string: Int, val note: Note) : Serializable {

    /**
     * This function finds the distance to a placement if it is the first one
     */
    internal fun startDistance(): Double {
        return fret * FRET_SCALING_FACTOR
        // high frets are punished for the purpose of encouraging the placements to be lower on the guitar
    }

    val correctedFret get() = fret - tuning.capo

    companion object {

        /**
         * The scaling factor for the distance function, used for optimising the distance function
         */
        private const val FRET_SCALING_FACTOR: Double = 2.0

        private const val INTERNAL_SCALING_FACTOR: Double = 1.0

        private const val MAX_FRET_SEPARATION = 5

        /**
         * The base in the exponential function relating to time's significance
         */
        private const val TIME_FACTOR_BASE = 1.04


        fun internalDistance(placements: List<Placement>): Double {

            return INTERNAL_SCALING_FACTOR * (placements.map { it.startDistance() }.average() + euclideanNorm(placements.map { it.fret }.range(), placements.map { it.string }.range()) * placements.size)

        }

        fun distance(firsts: List<Placement>, seconds: List<Placement>): Double {

            return firsts.flatMap { first ->
                seconds.map { second ->
                    distance(first, second)
                }
            }.average()

        }

        fun isPossible(placements: List<Placement>, pattern: Section.ChordPattern?): Boolean {

            val strings = placements.map { it.string }
            val stringSep = strings.range()
            val fretSep = placements.map { it.fret }.range()
            return when {
                strings.distinct().size != strings.size -> false
                pattern?.maxStringSep != null && stringSep > pattern.maxStringSep -> false
                fretSep > MAX_FRET_SEPARATION -> false
                else -> true
            }

        }

        private fun distance(a: Placement, b: Placement): Double {

            return if ((a.string - b.string) in -1..1 && (a.correctedFret == 0 || b.correctedFret == 0)) 0.0
            else {
                val fretRange = abs(a.fret - b.fret) * FRET_SCALING_FACTOR
                val stringRange = abs(a.fret - b.fret)
                val timeRange = abs(a.note.start - b.note.start)
                euclideanNorm(fretRange, stringRange) * timeDistance(timeRange)
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

        /**
         * The Euclidean normal returns the distance in euclidean space given the difference in each dimension.
         * e.g. (3,4) -> √(3²+4²) = 5
         * @param dims the variable argument list of distances in each dimension
         * @return the euclidean distance
         */
        private fun euclideanNorm(vararg dims: Number): Double = Math.sqrt(dims.map { it.toDouble() }.map { it.pow(2) }.sum())

    }

}

private fun List<kotlin.Int>.range(): kotlin.Int {
    val from = this.min()
    val to = this.max()
    return if (from != null && to != null) to - from else 0

}
