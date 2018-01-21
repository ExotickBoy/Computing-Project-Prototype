package core

import java.util.*


/**
 * This data class is the output of each TimeStep created by the model
 *
 * @author Kacper Lubisz
 *
 * @see Model
 * @see TimeStep
 *
 * @property predictions The list of predictions for each pitch created by the model
 * @property spectrum The spectrum of the samples of a TimeStep
 * @property dePhased The dePahsed visualisation of the TimeStep
 */
internal class StepOutput(val predictions: FloatArray, val spectrum: FloatArray, val dePhased: FloatArray) {

    var pitches: List<Int>

    init {

        pitches = predictions.mapIndexed { index, confidence -> index to confidence }
                .filter { it.second >= Model.CONFIDENCE_CUT_OFF }
                .map {
                    it.first + Model.START_PITCH
                }

    }

}