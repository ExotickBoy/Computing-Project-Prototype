package components

import core.Session
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.JPanel
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

internal class PhaserPane internal constructor(private val session: Session) : JPanel() {

    private var scale: Float = 0f
    private var colour: Float = 0f

    init {

        preferredSize = Dimension(500, 150)

        session.addOnStepChange { repaint() }
        session.addOnCursorChange {
            scale = 1f + ANIMATION_TIME / IMMUNE_TIME
            repaint()
        }

        thread(name = "Animation Thread") {

            val period = 1000 / ANIMATION_REFRESH_RATE
            var last = System.currentTimeMillis()
            var current = last
            var accumulated = 0.0

            while (true) {

                last = current
                current = System.currentTimeMillis()
                accumulated += current - last

                while (accumulated > period) {
                    accumulated -= period
                    if (session.isRecording) {

                        val scaleBefore = scale
                        val colourBefore = colour

                        scale = 1f + ANIMATION_TIME / IMMUNE_TIME
                        colour = scale

                        if (scale != scaleBefore || colour != colourBefore) {
                            repaint()
                        }

                    } else {

                        val scaleBefore = scale
                        val colourBefore = colour

                        scale = max(0f, scale - ANIMATION_STEP)
                        colour = max(0f, colour - ANIMATION_STEP)

                        if (scale != scaleBefore || colour != colourBefore) {
                            repaint()
                        }

                    }
                }

            }

        }

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

//                    println("${section.timeStepRange} ${session.correctedStepCursor}")

                    section.timeSteps[session.correctedStepCursor - section.timeStepStart]
                } else {
                    null
                }
            }

            if (currentStep != null) {

                val graph = Path2D.Double()

                val resolution = 1
                for (i in 0 until currentStep.dePhased.size step resolution) {

                    val x = size.getWidth() * i / currentStep.dePhased.size
                    if (i == 0) {

                        graph.moveTo(x, size.height / 2 + currentStep.dePhased[i] * SCALE * min(1f, scale))

                    } else {

                        graph.lineTo(x, size.height / 2 + currentStep.dePhased[i] * SCALE * min(1f, scale))

                    }

                }
                g.color = interpolateColour(g.color, Color.RED, min(colour, 1f))
                g.draw(graph)

            } else {

                g.draw(Line2D.Double(0.0, height / 2.0, width.toDouble(), height / 2.0))

            }

        }

    }

    companion object {

        private const val SCALE = 250.0

        private const val ANIMATION_TIME = .4f
        private const val IMMUNE_TIME = .8f
        private const val ANIMATION_REFRESH_RATE = 60
        private const val ANIMATION_STEP = (1 / ANIMATION_TIME / ANIMATION_REFRESH_RATE)

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

}
