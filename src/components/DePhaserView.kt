package components

import core.MainApplication
import core.Session
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.max
import kotlin.math.min

internal class DePhaserView(private val application: MainApplication, private val session: Session) : Canvas(500.0, 150.0) {

    private var scale: Double = 0.0
    private var colour: Double = 0.0

    private val animationThread = AnimationThread(this)

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

    fun end() {
        animationThread.interrupt()
    }

    private fun redraw() {
        Platform.runLater {
            draw()
        }
    }

    private fun draw(g: GraphicsContext = graphicsContext2D) {

        g.clearRect(0.0, 0.0, width, height)

        g.lineWidth = 0.45
        g.stroke = Color.GRAY.interpolate(Color.RED, min(colour, 1.0))

        synchronized(session.recording) {

            val currentStep = if (session.stepCursor == null) {
                session.recording.sections.findLast { it.timeSteps.isNotEmpty() }?.timeSteps?.last()
            } else {
                val sectionIndex = session.recording.sectionAt(session.correctedStepCursor)
                if (sectionIndex != null) {
                    val section = session.recording.sections[sectionIndex]

                    section.timeSteps[session.correctedStepCursor - section.timeStepStart]
                } else {
                    null
                }
            }

            if (currentStep != null) {

                val multiplier = MULTIPLIER * height * min(1.0, scale) * (2 - min(1.0, scale))

                g.beginPath()

                for (i in 0 until currentStep.dePhased.size step RESOLUTION) {

                    val x = width * i / currentStep.dePhased.size
                    if (i == 0) {
                        g.moveTo(x, height / 2 + currentStep.dePhased[i] * multiplier)
                    } else {
                        g.lineTo(x, height / 2 + currentStep.dePhased[i] * multiplier)
                    }

                }
                g.stroke()

            } else {

                g.strokeLine(0.0, height / 2, width, height / 2)

            }

        }

    }

    private class AnimationThread(val phaserPane: DePhaserView) : Thread("Animation Thread") {

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
                    if (!phaserPane.session.recording.isGathered) {

                        val scaleBefore = phaserPane.scale
                        val colourBefore = phaserPane.colour

                        phaserPane.scale = 1f + ANIMATION_TIME / IMMUNE_TIME
                        phaserPane.colour = phaserPane.scale

                        if (phaserPane.scale != scaleBefore || phaserPane.colour != colourBefore) {
                            phaserPane.redraw()
                        }

                    } else {

                        val scaleBefore = phaserPane.scale
                        val colourBefore = phaserPane.colour

                        phaserPane.scale = max(0.0, phaserPane.scale - ANIMATION_STEP)
                        phaserPane.colour = max(0.0, phaserPane.colour - ANIMATION_STEP)

                        if (phaserPane.scale != scaleBefore || phaserPane.colour != colourBefore) {
                            phaserPane.redraw()
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
