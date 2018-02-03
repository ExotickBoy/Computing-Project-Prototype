package core

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import javax.imageio.ImageIO
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
class TimeStep private constructor(val section: Section, private val sampleStart: Int, private val time: Int, private val previous: TimeStep? = null)
    : Serializable { // start in steps

    constructor(section: Section, sampleStart: Int, previous: TimeStep?) :
            this(section, sampleStart, (previous?.time ?: -1) + 1, previous)

    private val modelOutput: StepOutput

    @Transient
    var melImage: BufferedImage
    @Transient
    var noteImage: BufferedImage // TODO this is only for debugging in the desktop version

    val dePhased
        get() = modelOutput.dePhased

    val dePhasedPower
        get() = modelOutput.dePahsedPower

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

        modelOutput = Model.feedForward(samples)

        // identify notes present in the current timestep and link them with the ones in the previous one to make one note object
        notes = if (previous == null) {
            modelOutput.pitches.map {
                Note(it, time, 1)
            }
        } else {
            modelOutput.pitches.map {
                if (previous.modelOutput.pitches.contains(it)) {
                    val note = previous.notes.find { p -> p.pitch == it }!!
                    note.duration++
                    return@map note
                } else {
                    Note(it, time, 1)
                }
            }
        }

        // This buffered image is a slice of the spectrogram at this time step
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

    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {
        output.defaultWriteObject()

        val resultImage = BufferedImage(1, melImage.height + noteImage.height, BufferedImage.TYPE_INT_RGB)
        val g = resultImage.graphics
        g.drawImage(melImage, 0, 0, null)
        g.drawImage(noteImage, 0, melImage.height, null)

        ImageIO.write(resultImage, "png", output) // this object isn't serializable


//        val images = mutableListOf(melImage, noteImage)
//        output.writeInt(images.size) // how many images are serialized?
//        for (eachImage in images) {
//            ImageIO.write(eachImage, "png", output) // png is lossless
//        }

    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()

//        val imageCount = input.readInt()
//        val images = ArrayList<BufferedImage>(imageCount)
//        for (i in 0 until imageCount) {
//            images.add(ImageIO.read(input))
//        }
//
////        melImage = images[0].getSubimage(0,0,1,10)
////        noteImage = images[1].getSubimage(0,0,1,10)
//
//        println(images)
//        melImage = images[0]
//        noteImage = images[0]

        val resultImage = ImageIO.read(input)

        melImage = resultImage.getSubimage(0, 0, 1, Model.MEL_BINS_AMOUNT)
        noteImage = resultImage.getSubimage(0, Model.MEL_BINS_AMOUNT, 1, resultImage.height - Model.MEL_BINS_AMOUNT)

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
                Color(70, 6, 90),
                Color(54, 91, 141),
                Color(47, 180, 124),
                Color(248, 230, 33)
        )

        private val infernoColourMap = arrayOf(
                Color(0.001462f, 0.000466f, 0.013866f),
                Color(0.335217f, 0.060060f, 0.428524f),
                Color(0.729909f, 0.212759f, 0.333861f),
                Color(0.975677f, 0.543798f, 0.043618f),
                Color(0.988362f, 0.998364f, 0.644924f)
        )

        /**
         * Makes a value between 0 and 1 to a colour for the heat map
         * @param x The value to be mapped
         * @param colours The list of colours that are to be interpolated between, by default colourMapColours
         */
        private fun mapToColour(x: Float, colours: Array<Color> = viridisColourMap): Int {
            val h = (x * (colours.size - 1)).toInt()
            val f = (x * (colours.size - 1)) % 1

            return if (h == colours.size - 1)
                rgbToInt(colours[h])
            else
                interpolateColourToInt(colours[h], colours[h + 1], f)
        }

        /**
         * Interpolates between two colours and then converts the resulting colour to the 32 bit integer that
         * represents that colour
         * @param a The start colour
         * @param b The end colour
         * @param x the point in the interpolation
         */
        private fun interpolateColourToInt(a: Color, b: Color, x: Float): Int {

            return rgbToInt(a.red * (1 - x) + b.red * x,
                    a.green * (1 - x) + b.green * x,
                    a.blue * (1 - x) + b.blue * x)

        }

        /**
         * A wrapper function which automatically casts floats to integers
         * @param r The amount of red, in 0f..255f
         * @param g The amount of green, in 0f..255f
         * @param b The amount of blue, in 0f..255f
         * @return the 32 bit representation of the colour
         */
        private fun rgbToInt(r: Float, g: Float, b: Float): Int = rgbToInt(r.toInt(), g.toInt(), b.toInt())

        /**
         * Convert a colour in rbg [0-255] format to the integer that represents that colour
         * @param r The amount of red, in 0..255
         * @param g The amount of green, in 0..255
         * @param b The amount of blue, in 0..255
         * @return the 32 bit representation of the colour
         */
        private fun rgbToInt(r: Int, g: Int, b: Int): Int = (r shl 16) or (g shl 8) or b

        /**
         * Converts the colour object into the 32 bit integer that represents it
         * @col The colour to be converted
         * @return the 32 bit representation of the colour
         */
        private fun rgbToInt(col: Color): Int = rgbToInt(col.red, col.green, col.blue)

    }

}