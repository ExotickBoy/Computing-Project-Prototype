package components

import core.Section
import core.Session
import core.Session.Companion.DELETE_DISTANCE
import core.Session.Companion.DELETE_SWAP_COLOUR
import core.Session.Companion.SECTION_SPLIT_COLOUR
import core.Session.Companion.SECTION_SPLIT_THICKNESS
import core.Session.Companion.SWAP_REPLACE_COLOUR
import core.Session.Companion.SWAP_SEPARATION_COLOUR
import core.Session.Companion.SWAP_SEPARATION_THICKNESS
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.WritableImage
import javafx.scene.transform.Transform
import kotlin.math.max
import kotlin.math.sign

/**
 * This view shows the history of the image visualisations
 *
 * @author Kacper Lubisz
 * @param preferredHeight The preferred height for this component
 * @param willSwap if the scroll controller can start a swap on this view
 * @property images the lambda that fetches the images from the section
 */
internal class HistoryView(
        private val session: Session,
        preferredHeight: Double,
        private val willSwap: Boolean,
        private val images: (Section) -> (List<WritableImage>)
) : Canvas() {

    init {

        width = RecordingEditPane.RECORDING_EDIT_PANE_WIDTH
        height = preferredHeight
        isFocusTraversable = true

        ScrollController(false, willSwap, this, session)
        session.addOnUpdate {
            redraw()
        }
        session.addOnEdited { redraw() }

        draw(graphicsContext2D)

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

        g.clearRect(0.0, 0.0, width, height)

        synchronized(session.recording) {

            g.lineWidth = SECTION_SPLIT_THICKNESS
            g.stroke = SECTION_SPLIT_COLOUR

            session.recording.sections.filterIndexed { index, _ ->
                !willSwap || index != session.swap
            }.forEach { section ->

                        // I have found that trying to draw an image off screen has a negligible effect on performance
                        // which is why I don't check if all of these need to be drawn
                        images.invoke(section).fold(0.0) { acc, image ->

                            g.drawImage(
                                    image,
                                    acc + section.timeStepStart - session.stepFrom.toDouble(),
                                    0.0,
                                    image.width,
                                    height
                            )

                            acc + image.width

                        }
                        /*
                            for (i in 0..10000)
                                g.drawImage(image, -1000, 0, image.width, height, null)

                            This is the aforementioned test, I didn't measure how much longer this takes, but after
                            running this code it was apparent that there was no performance decrease even in this
                            extreme case
                         */
                        if (section.clusterStart != 0)
                            g.strokeLine(section.timeStepStart - session.stepFrom + 0.5, 0.0, section.timeStepStart - session.stepFrom + 0.5, height)

                    }

            g.lineWidth = Session.CURSOR_THICKNESS
            g.stroke = Session.CURSOR_COLOUR
            g.strokeLine(session.onScreenStepCursor.toDouble(), 0.0, session.onScreenStepCursor.toDouble(), height)

            val swap = session.swap
            val swapWith = session.swapWith
            // I am making these local variables because making them final means that they are automatically cast as none null

            if (willSwap && swap != null) { // draw the floating swap

                when {
                    session.swapWithSection == true -> {

                        val sectionTo = session.recording.sections[swapWith]
                        val from = sectionTo.timeStepStart - session.stepFrom.toDouble()


                        g.fill = SWAP_REPLACE_COLOUR
                        g.fillRect(from, 0.0, sectionTo.timeStepLength.toDouble(), height)

                    }
                    session.swapWithSection == false -> {

                        val from: Double
                        from = if (swapWith == session.recording.sections.size) {
                            session.recording.sections.last().timeStepEnd
                        } else {
                            val sectionTo = session.recording.sections[swapWith]
                            sectionTo.timeStepStart - session.stepFrom
                        }.toDouble()

                        g.stroke = SWAP_SEPARATION_COLOUR
                        g.lineWidth = SWAP_SEPARATION_THICKNESS
                        g.strokeLine(from, 0.0, from, height)

                    }
                    else -> {

                        g.fill = DELETE_SWAP_COLOUR
                        g.fillRect(0.0, 0.0, width, height * DELETE_DISTANCE / 2)
                    }
                }

                val section = session.recording.sections[swap]

                val transformBefore = g.transform
                val y = height * (max(-session.lastY / (2 * DELETE_DISTANCE) + 0.5, 0.0) * sign(session.lastY - 0.5) + 0.1)
                // this function determines its vertical location
                g.transform(Transform.affine(1.0, 0.0, 0.0, 0.8, 0.0, y))
                // rescale and move it

                val image = images.invoke(section)[0]

                g.drawImage(
                        image,
                        session.lastX.toDouble(),
                        0.0,
                        image.width,
                        height
                )

                g.transform = transformBefore

            }

        }

    }

}
