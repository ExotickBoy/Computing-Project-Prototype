package components

import core.Session
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min

/**
 * This class shows the phase removed visualisation of the current step
 * @author Kacper Lubisz
 */
internal class DePhaserView(private val session: Session) : Canvas(500.0, 150.0) {

    private var scale: Double = 0.0 /* This indicates the progress of the collapse animation of the wave, it decreases quickly over time */
    private var colour: Double = 0.0 /* Indicates the colour of the wave, it is red when recording*/

    private val animationThread = AnimationThread(this) /*Updates the animation*/

    init {

        widthProperty().addListener { _ ->
            redraw()
        }
        heightProperty().addListener { _ -> redraw() }

        session.addOnUpdate {
            scale = 1 + ANIMATION_TIME / IMMUNE_TIME
            redraw()
        }

        animationThread.start()

    }

    /**
     * Kills the animation thread
     */
    fun end() {
        animationThread.interrupt()
    }

    /**
     * Repaints the canvas
     */
    private fun redraw() {
        Platform.runLater {
            draw()
        }
    }

    /**
     * Draws the components on the canvas
     * @param g the graphics context
     */
    private fun draw(g: GraphicsContext = graphicsContext2D) {

        g.clearRect(0.0, 0.0, width, height)

        g.lineWidth = 0.45
        g.stroke = Color.GRAY.interpolate(Color.RED, min(colour, 1.0))

        synchronized(session.recording) {

            val current = if (session.stepCursor == null) {
                session.recording.sections.findLast { it.timeStepLength != 0 }?.dePhased?.last()
            } else {
                val sectionIndex = session.recording.sectionAt(session.correctedStepCursor)
                if (sectionIndex != null) {
                    val section = session.recording.sections[sectionIndex]
                    section.dePhased[session.correctedStepCursor - section.timeStepStart]
                } else {
                    null
                }
            }

            if (current != null) {

                val multiplier = MULTIPLIER * height * min(1.0, scale) * (2 - min(1.0, scale))

                g.beginPath()

                for (i in 0 until current.size step RESOLUTION) {

                    val x = width * i / current.size
                    if (i == 0) {
                        g.moveTo(x, height / 2 + current[i] * multiplier)
                    } else {
                        g.lineTo(x, height / 2 + current[i] * multiplier)
                    }

                }
                g.stroke()

            } else {

                g.strokeLine(0.0, height / 2, width, height / 2)

            }

        }

    }

    /**
     *  This thread is responsible for updating the value of the animation properties
     */
    private class AnimationThread(val dePhaserPane: DePhaserView) : Thread("Animation Thread") {

        override fun run() {

            val period = 1000 / ANIMATION_REFRESH_RATE
            var last = System.currentTimeMillis()
            var current = last
            var accumulated = 0.0

            while (!isInterrupted) {

                last = current
                current = System.currentTimeMillis()
                accumulated += current - last

                while (accumulated > period) {
                    accumulated -= period
                    if (!dePhaserPane.session.recording.isGathered) {

                        val scaleBefore = dePhaserPane.scale
                        val colourBefore = dePhaserPane.colour

                        dePhaserPane.scale = 1f + ANIMATION_TIME / IMMUNE_TIME
                        dePhaserPane.colour = dePhaserPane.scale

                        if (dePhaserPane.scale != scaleBefore || dePhaserPane.colour != colourBefore) {
                            dePhaserPane.redraw()
                        }

                    } else {

                        val scaleBefore = dePhaserPane.scale
                        val colourBefore = dePhaserPane.colour

                        dePhaserPane.scale = max(0.0, dePhaserPane.scale - ANIMATION_STEP)
                        dePhaserPane.colour = max(0.0, dePhaserPane.colour - ANIMATION_STEP)

                        if (dePhaserPane.scale != scaleBefore || dePhaserPane.colour != colourBefore) {
                            dePhaserPane.redraw()
                        }

                    }
                }

            }

        }

    }

    companion object {

        private const val ANIMATION_TIME = .4
        private const val IMMUNE_TIME = .8
        private const val ANIMATION_REFRESH_RATE = 60
        private const val ANIMATION_STEP = (1 / ANIMATION_TIME / ANIMATION_REFRESH_RATE)
        private const val RESOLUTION = 1
        private const val MULTIPLIER = 7.5

    }

}
