package core

import components.NoteOutputView.Companion.color
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min


/**
 * This class is for storing the data related to each section of time.
 * This class is only used to temporarily store the information before it is added to a section of recording.
 * This is because storing lots of these time steps was inefficient when it came to writing to file
 *
 * @author Kacper Lubisz
 *
 * @see Model
 * @see Model.StepOutput
 * @see Note
 *
 * @property section The section that this time step belongs to
 * @property sampleStart The sample at which the frame of this step starts
 * @property time The time which the step represents in relation to the current section
 * @property melImage The image that can be drawn on screen of the frequencies on the mel scale
 * @property noteImage The image that can be drawn on screen of the output of the neural network
 * @property dePhased The reconstructed samples after the phase of the sinusoid that make it up is removed
 * @property dePhasedPower This value represents the volume of the TimeStep
 * @property pitches The pitches that are present in the time step
 * @property notes The notes(linked with the same notes in the previous step) that are present in the time step
 */
class TimeStep private constructor(val section: Section, private val sampleStart: Int, private val time: Int, previous: TimeStep? = null) { // start in steps

    constructor(section: Section, sampleStart: Int, previous: TimeStep?) :
            this(section, sampleStart, (previous?.time ?: -1) + 1, previous)

    var melImage: WritableImage
    var noteImage: WritableImage

    val dePhased: FloatArray
    val dePhasedPower: Float

    private val pitches: List<Int>

    private val samples: FloatArray
        get() {
            synchronized(section.recording) {
                return section.samples.subList(sampleStart, sampleStart + SoundProcessingController.FRAME_SIZE).toFloatArray()
            }
        }

    val notes: List<Note>

    init {

        val samples = samples
        // so that the samples don't need to be sub-listed twice

        if (time == 0) {
            Model.setQueue(samples)
        } // resets the sample input queue in the tensorflow session

        val (predictions, spectrum, dePhased, dePhasedPower) = Model.feedForward(samples)
        // pass the samples through the neural network
        this.dePhased = dePhased
        this.dePhasedPower = dePhasedPower

        pitches = predictions.mapIndexed { index, confidence -> index to confidence }
                .filter {
                    return@filter it.second >= Model.CONFIDENCE_CUT_OFF
                }.map {
                    it.first + Model.START_PITCH
                }

        // identify notes present in the current timestep and link them with the ones in the previous one to make one note object
        notes = if (previous == null) {
            pitches.map {
                Note(it, time, 1)
            }
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

        // This image is a slice of the spectrogram at this time step
        melImage = WritableImage(1, Model.MEL_BINS_AMOUNT)
        for (y in 0 until Model.MEL_BINS_AMOUNT) {
            val value = ((min(max(spectrum[y], minMagnitude), maxMagnitude) - minMagnitude) / (maxMagnitude - minMagnitude))
            melImage.pixelWriter.setColor(0, Model.MEL_BINS_AMOUNT - y - 1, mapToColour(value.toDouble()))
        }
        noteImage = WritableImage(1, Model.PITCH_RANGE)
        for (y in 0 until Model.PITCH_RANGE) {
            val value = min(max(predictions[y], 0f), 1f)
            noteImage.pixelWriter.setColor(0, Model.PITCH_RANGE - y - 1, mapToColour(value.toDouble()))
        }

    }

    companion object {

        /**
         * This is the range between which the volumes should be interpolated
         */
        private const val maxMagnitude = -1.0f
        private const val minMagnitude = -16.0f

        /**
         * The colours can interpolate between to create a scale.
         * I chose these colors because I wanted to reduce the amount of colours that the heat map uses so that
         * I can use other colours over it
         */
        private val viridisColourMap: Array<Color> = arrayOf(
                color(70, 6, 90),
                color(54, 91, 141),
                color(47, 180, 124),
                color(248, 230, 33)
        )


        /**
         * Makes a value between 0 and 1 to a colour for the heat map
         * @param x The value to be mapped
         * @param colours The list of colours that are to be interpolated between, by default colourMapColours
         */
        private fun mapToColour(x: Double, colours: Array<Color> = viridisColourMap): Color {
            val h = (x * (colours.size - 1)).toInt()
            val f = (x * (colours.size - 1)) % 1

            return if (h == colours.size - 1)
                colours[h]
            else
                colours[h].interpolate(colours[h + 1], f)
        }

    }

}