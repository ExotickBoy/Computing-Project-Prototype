package components

import components.RecordingEditPane.Companion.overlap
import core.ChordPattern
import core.Model
import core.Note.Companion.noteStringShort
import core.Session
import core.Session.Companion.CURSOR_COLOUR
import core.Session.Companion.CURSOR_THICKNESS
import core.Session.Companion.SECTION_SPLIT_COLOUR
import core.Session.Companion.SECTION_SPLIT_THICKNESS
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.transform.Affine
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * This class displays the tuning and the output of the clustering
 *
 * @author Kacper Lubisz
 *
 */
internal class NoteOutputView(private val session: Session) : Canvas() {

    private var lineHeight: Double = 0.0 /*The height of each string*/
    private val margin: Double /*The width of the left tuning string axis */

    private val headerHeight: Double /* The height of the top line where the chord names go*/

    val spacing: Double /* The horizontal distance between notes */

    init {

        widthProperty().addListener { _ ->
            updateSize()
        }
        heightProperty().addListener { _ ->
            updateSize()
        }

        margin = session.recording.tuning.strings
                .map { it.noteStringShort }
                .map { stringWidth(it, graphicsContext2D) }
                .max() ?: 0.0

        spacing = ((session.recording.tuning.capo..session.recording.tuning.maxFret).map {
            stringWidth(it.toString(), graphicsContext2D)
        }.max() ?: 0.0) + PADDING / 2
        headerHeight = (Model.START_PITCH..Model.END_PITCH).flatMap {
            return@flatMap ChordPattern.values().map { chordPattern ->
                return@map "${it.noteStringShort}${chordPattern.suffix}"
            }
        }.map { stringWidth(it, graphicsContext2D) }.max() ?: 0.0

        ScrollController(true, true, this, session)

        session.addOnUpdate { redraw() }
        session.addOnEdited { redraw() }

        width = RecordingEditPane.RECORDING_EDIT_PANE_WIDTH
        height = session.recording.tuning.size * PREFERRED_LINE_HEIGHT + headerHeight

    }

    /**
     *  Finds the width in pixels of a string
     *  @param message the string who's width will be found
     *  @param g the graphics context
     *  @return the width
     */
    private fun stringWidth(message: String, g: GraphicsContext): Double {
        val text = Text(message)
        text.font = g.font
        return text.layoutBounds.width
    }

    /**
     * Repaints the canvas
     */
    private fun redraw() {
        Platform.runLater {
            // this is because drawing can only be done from the JavaFx thread, and this may be done from an external one
            // this allows redraw to be called from any thread
            draw()
        }
    }

    /**
     * Draws the components on the canvas
     * @param g the graphics context
     */
    private fun draw(g: GraphicsContext = graphicsContext2D) {


        synchronized(session.recording) {

            val stringHeaderOffset = min(max((session.clusterWidth / 2 - session.onScreenClusterCursor) * spacing, 0.0), margin + 2.0 * PADDING)

            // stripes
            g.fill = STRIPE_LIGHT
            g.fillRect(0.0, 0.0, width, headerHeight)
            (0 until session.recording.tuning.size).forEach { index ->
                g.fill = if (index % 2 == 1) STRIPE_LIGHT else STRIPE_DARK
                g.fillRect(0.0, index * lineHeight + headerHeight, width, lineHeight)
            }

            // sections and their notes

            session.recording.sections.forEach {

                val doubleRange = it.clusterRange.toDoubleRange() overlap session.visibleClusterRange
                val clusterRange = floor(doubleRange.start).toInt()..ceil(doubleRange.endInclusive).toInt()

                it.clusters.forEachIndexed { index, cluster ->

                    if (index + it.clusterStart in clusterRange) {
                        g.fill = TEXT_COLOUR
                        cluster.placements.forEach { placement ->
                            // draw a cluster
                            val asPlacementObject = session.recording.tuning.placements[placement]
                            g.fillText(
                                    asPlacementObject.fret.toString(),
                                    (stringHeaderOffset).toFloat() + (it.clusterStart - session.clusterFrom + 0.5f + index) * spacing - stringWidth(asPlacementObject.fret.toString(), g) / 2,
                                    (lineHeight * (asPlacementObject.string + 1) - (lineHeight - g.font.size) / 2 + headerHeight)
                            )

                        }

                        val transformBefore = g.transform
                        g.transform(Affine( // rotation through 90 degrees
                                0.0,
                                1.0,
                                stringHeaderOffset + (it.clusterStart - session.clusterFrom + index) * spacing,
                                -1.0,
                                0.0,
                                headerHeight
                        ))


                        g.fill = if (cluster.pattern != null) TEXT_COLOUR else DARK_TEXT_COLOUR

                        g.fillText(
                                cluster.heading,
                                (headerHeight - stringWidth(cluster.heading, g)) / 2,
                                (g.font.size + (spacing - g.font.size) / 2)
                        ) // draw heading
                        g.transform = transformBefore //making sure that I reset the transformation I set so that the rest of the ui doesn't renter incorrectly

                    }
                }

                if (it.timeStepLength != 0 && it.timeStepStart != 0) { // doesn't draw a separation at the beginning of if there are no notes

                    g.stroke = SECTION_SPLIT_COLOUR
                    g.lineWidth = SECTION_SPLIT_THICKNESS
                    g.strokeLine(
                            stringHeaderOffset + (it.clusterStart - session.clusterFrom) * spacing,
                            0.0,
                            stringHeaderOffset + (it.clusterStart - session.clusterFrom) * spacing,
                            height
                    )

                }

            }

            // Tuning header

            g.fill = STRIPE_LIGHT
            g.fillRect(-(margin + 2 * PADDING) + stringHeaderOffset, 0.0, margin + 2.0 * PADDING, headerHeight)
            for (index in 0 until session.recording.tuning.size) {
                g.fill = TEXT_COLOUR

                g.fillText(
                        (session.recording.tuning[index] + session.recording.tuning.capo).noteStringShort,
                        (-(margin + 2 * PADDING) + stringHeaderOffset + PADDING),
                        (lineHeight * (index + 1) + headerHeight - (lineHeight - g.font.size) / 2)
                )

            }
            g.stroke = STRIPE_LIGHT
            g.strokeLine(stringHeaderOffset, headerHeight, stringHeaderOffset, height)

            // Cursor


            g.lineWidth = CURSOR_THICKNESS
            g.stroke = CURSOR_COLOUR

            g.strokeLine(
                    stringHeaderOffset + session.onScreenClusterCursor * spacing,
                    0.0,
                    stringHeaderOffset + session.onScreenClusterCursor * spacing,
                    height
            )

        }

    }

    /**
     * Refreshes the session's width when the element resizes
     */
    private fun updateSize() {

        synchronized(session.recording) {

            lineHeight = (height - headerHeight) / session.recording.tuning.size.toDouble()

            session.width = width.toInt()
            session.clusterWidth = width / spacing

        }

    }

    /**
     * Converts an int range to a double range
     */
    private fun IntRange.toDoubleRange(): ClosedFloatingPointRange<Double> = start.toDouble()..endInclusive.toDouble()

    companion object {

        private const val PREFERRED_LINE_HEIGHT = 20
        private val STRIPE_LIGHT = color(232, 232, 232)
        private val STRIPE_DARK = color(245, 245, 245)

        private val TEXT_COLOUR = color(86, 86, 86)
        private val DARK_TEXT_COLOUR = color(150, 150, 150)

        private const val PADDING = 5.0

        /**
         * A convenient extension function for convenient int rgp to a Colour object
         */
        fun color(r: Int, g: Int, b: Int): Color = Color.color(r / 255.0, g / 255.0, b / 255.0)


    }

}