package core

import java.util.*


/**
 * This data class is the output of each TimeStep created by the model
 * @see Model
 * @see TimeStep
 * @author Kacper Lubisz
 * @property predictions The list of predictions for each pitch created by the model
 * @property spectrum The spectrum of the samples of a TimeStep
 */
data class StepOutput(val predictions: FloatArray, val spectrum: FloatArray, val depased: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StepOutput

        if (!Arrays.equals(predictions, other.predictions)) return false
        if (!Arrays.equals(spectrum, other.spectrum)) return false
        if (!Arrays.equals(depased, other.depased)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(predictions)
        result = 31 * result + Arrays.hashCode(spectrum)
        result = 31 * result + Arrays.hashCode(depased)
        return result
    }
}