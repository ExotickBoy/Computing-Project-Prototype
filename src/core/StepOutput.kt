package core


/**
 * This data class is the output of each TimeStep created by the model
 * @see Model
 * @see TimeStep
 * @author Kacper Lubisz
 * @property predictions The list of predictions for each pitch created by the model
 * @property spectrum The spectrum of the samples of a TimeStep
 */
data class StepOutput(val predictions: FloatArray, val spectrum: FloatArray)