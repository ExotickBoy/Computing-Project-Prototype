package core

import components.NoteOutputView.Companion.color
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min


/**
 * This class is for storing the data related to each section of time.
 * This object interacts with the model by passing the samples to it.
 *
 * @author Kacper Lubisz
 *
 * @see Model
 * @see StepOutput
 * @see Note
 *
 * @property section The section that this time step belongs to
 * @property dePhased The reconstructed samples after the phase of the sinusoid that make it up is removed
 * @property melImage The image that can be drawn on screen of the frequencies on the mel scale
 * @property modelOutput The object that represents the outputs of the Model
 * @property notes The notes that are present in the time step
 */
class TimeStep private constructor(val section: Section, private val sampleStart: Int, private val time: Int, private val previous: TimeStep? = null) { // start in steps

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
        }

        val (predictions, spectrum, dePhased, dePhasedPower) = Model.feedForward(samples)
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
            melImage.pixelWriter.setColor(0, y, mapToColour(value.toDouble()))
        }
        noteImage = WritableImage(1, Model.PITCH_RANGE)
        for (y in 0 until Model.PITCH_RANGE) {
            val value = min(max(predictions[y], 0f), 1f)
            noteImage.pixelWriter.setColor(0, y, mapToColour(value.toDouble()))
        }

//        this.melImage = SwingFXUtils.toFXImage(melImage, null)
//        this.noteImage = SwingFXUtils.toFXImage(noteImage, null)

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