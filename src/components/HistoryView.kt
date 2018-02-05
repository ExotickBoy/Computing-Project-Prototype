package components

import core.Section
import core.Session
import core.Session.Companion.DELETE_DISTANCE
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.transform.Transform
import kotlin.math.max
import kotlin.math.sign


internal class HistoryView(
        private val session: Session,
        preferredHeight: Double,
        private val willSwap: Boolean,
        private val images: (Section) -> (List<WritableImage>)
) : Canvas() {

    init {

        width = 500.0
        height = preferredHeight
        isFocusTraversable = true

        ScrollController(false, willSwap, this, session)
        session.addOnUpdate {
            redraw()
        }
        session.addOnEdited { redraw() }

        draw(graphicsContext2D)

    }

    private fun redraw() {
        Platform.runLater {
            // this is because drawing can only be done from the JavaFx thread, and this may be done from an external one
            // this allows redraw to be called from any thread
            draw()
        }
    }

    private fun draw(g: GraphicsContext = graphicsContext2D) {

        g.clearRect(0.0, 0.0, width, height)

        synchronized(session.recording) {

            g.lineWidth = 1.0
            g.stroke = Color.MAGENTA

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

            g.lineWidth = 2.0
            g.stroke = Color.RED
            g.strokeLine(session.onScreenStepCursor.toDouble(), 0.0, session.onScreenStepCursor.toDouble(), height)

            val swap = session.swap
            val swapWith = session.swapWith
            // I am making these local variables because making them final means that they are automatically cast as none null

            if (willSwap && swap != null) {

                when {
                    session.swapWithSection == true -> {

                        val sectionTo = session.recording.sections[swapWith]
                        val from = sectionTo.timeStepStart - session.stepFrom.toDouble()

                        g.fill = Color(0.0, 1.0, 0.0, .5)
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

                        g.stroke = Color.GREEN
                        g.lineWidth = 2.0
                        g.strokeLine(from, 0.0, from, height)

                    }
                    else -> {
                        g.fill = Color(1.0, 0.0, 0.0, 0.5)
                        g.fillRect(0.0, 0.0, width, height * DELETE_DISTANCE / 2)
                    }
                }

                val section = session.recording.sections[swap]

                val transformBefore = g.transform
                val y = height * (max(-session.lastY / (2 * DELETE_DISTANCE) + 0.5, 0.0) * sign(session.lastY - 0.5) + 0.1)
                g.transform(Transform.affine(1.0, 0.0, 0.0, 0.8, 0.0, y))

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