package core

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

/**
 * This class is for storing the data related to each section of time.
 * This object interacts with the model by passing the samples to it.
 * @author Kacper Lubisz
 * @see Model
 * @see StepOutput
 * @see Note
 * @property dePhased The reconstructed samples after the phase of the sinusoid that make it up is removed
 * @property melImage The image that can be drawn on screen of the frequencies on the mel scale
 * @property modelOutput The object that represents the outputs of the Model
 * @property notes The notes that are present in the time step
 */
class TimeStep private constructor(samples: FloatArray, private val time: Int, private val previous: TimeStep? = null) { // start in steps

    constructor(samples: FloatArray, previous: TimeStep?) : this(samples, (previous?.time ?: -1) + 1, previous)

    private val modelOutput: StepOutput = Model.feedForward(samples)

    val melImage: BufferedImage
    val noteImage: BufferedImage // TODO this is only for debugging in the desktop version

    val pitches: List<Int> // TODO move this into step output
    val notes: List<Note>

    val dePhased: FloatArray = modelOutput.depased

    init {

        pitches = modelOutput.predictions
                .mapIndexed { index, confidence -> index to confidence }
                .filter { it.second >= Model.CONFIDENCE_CUT_OFF }
                .map { it.first + Model.START_PITCH }

        notes = if (previous == null) {
            pitches.map { Note(it, time, 1) }
        } else {
            pitches.map {
                if (previous.pitches.contains(it)) {
                    val note = previous.notes.find { p -> p.pitch == it }!!
                    note.duration++
                    return@map note
                } else {
                    Note(it, time, 1)
                }
            }
        }


        melImage = BufferedImage(1, Model.MEL_BINS_AMOUNT, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until Model.MEL_BINS_AMOUNT) {
            val value = ((min(max(modelOutput.spectrum[y], minMagnitude), maxMagnitude) - minMagnitude) / (maxMagnitude - minMagnitude))
            melImage.setRGB(0, y, mapToColour(value))
        }
        noteImage = BufferedImage(1, Model.PITCH_RANGE, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until Model.PITCH_RANGE) {
            val value = min(max(modelOutput.predictions[y], 0f), 1f)
            noteImage.setRGB(0, y, mapToColour(value))
        }

    }

    companion object {

        private const val maxMagnitude = 0.0f
        private const val minMagnitude = -13.0f

        private val colourMapColours: Array<Color> = arrayOf(
                Color(70, 6, 90),
                Color(54, 91, 141),
                Color(47, 180, 124),
                Color(248, 230, 33)
        )

        private fun mapToColour(x: Float, colours: Array<Color> = colourMapColours): Int {
            val h = (x * (colours.size - 1)).toInt()
            val f = (x * (colours.size - 1)) % 1

            return if (h == colours.size - 1)
                rgbToInt(colours[h])
            else
                interpolateColourToInt(colours[h], colours[h + 1], f)
        }

        private fun interpolateColourToInt(a: Color, b: Color, x: Float): Int {

            return rgbToInt(a.red * (1 - x) + b.red * x,
                    a.green * (1 - x) + b.green * x,
                    a.blue * (1 - x) + b.blue * x)

        }

        private fun rgbToInt(r: Float, g: Float, b: Float): Int = rgbToInt(r.toInt(), g.toInt(), b.toInt())

        /**
         * Convert a colour in rbg [0-255] format to the integer that represents that colour
         * @param r The amount of red, in 0..255
         * @param g The amount of green, in 0..255
         * @param b The amount of blue, in 0..255
         * @return the 32 bit representation of the colour
         */
        private fun rgbToInt(r: Int, g: Int, b: Int): Int = (r shl 16) or (g shl 8) or b

        private fun rgbToInt(col: Color): Int = rgbToInt(col.red, col.green, col.blue)

    }

}