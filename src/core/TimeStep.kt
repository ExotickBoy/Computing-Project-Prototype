package core

import java.awt.image.BufferedImage
import java.lang.Float.max
import java.lang.Float.min

class TimeStep(samples: FloatArray, val time: Int, val previous: TimeStep? = null) { // start in steps

    val magnitudes: Array<Double> = FFT.fft(samples).map(Complex::magnitude).toTypedArray()
    val dePhased: Array<Double>

    val melImage: BufferedImage
    val noteImage: BufferedImage

    val modelOutput: StepOutput = Model.feedForward(samples)

    val pitches: List<Int>
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
            val hue = (1 - (min(max(modelOutput.spectrum[y], min), max) - min) / (max - min)) * 2.0 / 3
            melImage.setRGB(0, y, hsvToInt(hue, 1, 1))
        }
        noteImage = BufferedImage(1, Model.PITCH_RANGE, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until Model.PITCH_RANGE) {
            val hue = (1 - min(max(modelOutput.predictions[y], 0f), 1f)) * 2.0 / 3
            noteImage.setRGB(0, y, hsvToInt(hue, 1, 1))
        }

    }

    companion object {

        const val max = 5.0f
        const val min = -20.0f

        private fun hsvToInt(hue: Number, saturation: Number, value: Number): Int = hsvToInt(hue.toDouble(), saturation.toDouble(), value.toDouble())
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

        private fun rgbToInt(r: Double, g: Double, b: Double): Int =
                (r * 255).toInt() shl 16 or ((g * 255).toInt() shl 8) or (b * 255).toInt()

    }

}