package components

import core.Session
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

internal class DePhaserPane internal constructor(private val session: Session) : JPanel() {

    private var scale: Float = 0f
    private var colour: Float = 0f

    private val animationThread = AnimationThread(this)

    init {

        minimumSize = Dimension(500, 150)

        session.addOnUpdate {
            scale = 1f + ANIMATION_TIME / IMMUNE_TIME
            repaint()
        }

        animationThread.start()

    }

    fun end() {
        animationThread.interrupt()
    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
        g.setRenderingHints(rh)

        val size = size
        g.stroke = BasicStroke(.45f)

//        val step = recording.sectionAt(session.cursor)

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

                val graph = Path2D.Double()

                val multiplier = MULTIPLIER * height * min(1f, scale) * (2 - min(1f, scale))

                for (i in 0 until currentStep.dePhased.size step RESOLUTION) {

                    val x = size.getWidth() * i / currentStep.dePhased.size
                    if (i == 0) {
                        graph.moveTo(x, size.height / 2 + currentStep.dePhased[i] * multiplier)
                    } else {
                        graph.lineTo(x, size.height / 2 + currentStep.dePhased[i] * multiplier)
                    }

                }
                g.color = interpolateColour(g.color, Color.RED, min(colour, 1f))
                g.draw(graph)

            } else {

                g.draw(Line2D.Double(0.0, height / 2.0, width.toDouble(), height / 2.0))

            }

        }

    }

    private class AnimationThread(val phaserPane: DePhaserPane) : Thread("Animation Thread") {

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
                            phaserPane.repaint()
                        }

                    } else {

                        val scaleBefore = phaserPane.scale
                        val colourBefore = phaserPane.colour

                        phaserPane.scale = max(0f, phaserPane.scale - ANIMATION_STEP)
                        phaserPane.colour = max(0f, phaserPane.colour - ANIMATION_STEP)

                        if (phaserPane.scale != scaleBefore || phaserPane.colour != colourBefore) {
                            phaserPane.repaint()
                        }

                    }
                }

            }

        }

    }

    companion object {

        private const val ANIMATION_TIME = .4f
        private const val IMMUNE_TIME = .8f
        private const val ANIMATION_REFRESH_RATE = 60
        private const val ANIMATION_STEP = (1 / ANIMATION_TIME / ANIMATION_REFRESH_RATE)
        private const val RESOLUTION = 1
        private const val MULTIPLIER = 7.5

        /**
         * Interpolates between two colours and then converts the resulting colour to the 32 bit integer that
         * represents that colour
         * @param a The start colour
         * @param b The end colour
         * @param x the point in the interpolation
         */
        private fun interpolateColour(a: Color, b: Color, x: Float): Color {

            return Color((a.red * (1 - x) + b.red * x).toInt(),
                    (a.green * (1 - x) + b.green * x).toInt(),
                    (a.blue * (1 - x) + b.blue * x).toInt())

        }

    }

    private fun Path2D.Double.lineTo(x: Number, y: Number) {
        this.lineTo(x.toDouble(), y.toDouble())
    }

    private fun Path2D.Double.moveTo(x: Number, y: Number) {
        this.moveTo(x.toDouble(), y.toDouble())
    }

}
