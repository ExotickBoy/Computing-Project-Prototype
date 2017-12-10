package core

import java.awt.image.BufferedImage
import java.lang.Float.max
import java.lang.Float.min

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

    val magnitudes: Array<Double> = FFT.fft(samples).map(Complex::magnitude).toTypedArray() // TODO remove this call
    val dePhased: Array<Double>

    val melImage: BufferedImage
    val noteImage: BufferedImage // TODO this is only for debugging in the desktop version

    val modelOutput: StepOutput = Model.feedForward(samples)

    val pitches: List<Int> // TODO move this into step output
    val notes: List<Note>

    init {

        dePhased = FFT.ifft(magnitudes.map { Complex(it, 0.0) }.toTypedArray()).map { it.re }.toTypedArray()

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
            val hue = (1 - (min(max(modelOutput.spectrum[y], minMagnitude), maxMagnitude) - minMagnitude) / (maxMagnitude - minMagnitude)) * 2.0 / 3
            melImage.setRGB(0, y, hsvToInt(hue, 1, 1))
        }
        noteImage = BufferedImage(1, Model.PITCH_RANGE, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until Model.PITCH_RANGE) {
            val hue = (1 - min(max(modelOutput.predictions[y], 0f), 1f)) * 2.0 / 3
            noteImage.setRGB(0, y, hsvToInt(hue, 1, 1))
        }

    }

    companion object {

        const val maxMagnitude = 5.0f
        const val minMagnitude = -20.0f

        /**
         * Converts the colour from hsv format to an int representation
         * @param h The value of hue, in 0..1
         * @param s The value of saturation, in 0..1
         * @param v The value of value, in 0..1
         * @return the 32 bit representation of the colour
         */
        private fun hsvToInt(hue: Number, saturation: Number, value: Number): Int = hsvToInt(hue.toDouble(), saturation.toDouble(), value.toDouble())

        /**
         * Converts the colour from hsv format to an int representation
         * @param h The value of hue, in 0..1
         * @param s The value of saturation, in 0..1
         * @param v The value of value, in 0..1
         * @return the 32 bit representation of the colour
         */
        private fun hsvToInt(hue: Double, saturation: Double, value: Double): Int {
            var h = (hue * 6).toInt()
            if (h == 6)
                h = 5
            val f = hue * 6 - h
            val p = value * (1 - saturation)
            val q = value * (1 - f * saturation)
            val t = value * (1 - (1 - f) * saturation)

            return when (h) {
                0 -> rgbToInt(value, t, p)
                1 -> rgbToInt(q, value, p)
                2 -> rgbToInt(p, value, t)
                3 -> rgbToInt(p, q, value)
                4 -> rgbToInt(t, p, value)
                5 -> rgbToInt(value, p, q)
                else -> throw RuntimeException("Something went wrong when converting from HSV to RGB. Input was $hue, $saturation, $value")
            }
        }

        /**
         * Convert a colour in rbg [0-1] format to the integer that represents that colour
         * @param r The amount of red, in 0..1
         * @param g The amount of green, in 0..1
         * @param b The amount of blue, in 0..1
         * @return the 32 bit representation of the colour
         */
        private fun rgbToInt(r: Double, g: Double, b: Double): Int =
                (r * 255).toInt() shl 16 or ((g * 255).toInt() shl 8) or (b * 255).toInt()

    }

}