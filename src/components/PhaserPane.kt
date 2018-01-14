package components

import core.Session
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.JPanel
import kotlin.concurrent.thread
import kotlin.math.max

internal class PhaserPane internal constructor(private val session: Session) : JPanel() {

    private var showing: Float = 0f

    init {

        preferredSize = Dimension(500, 150)

        session.addOnStepChange { repaint() }

        thread(name = "Animation Thread") {

            val period = 1000 / ANIMATION_REFRESH_RATE
            var last = System.currentTimeMillis();
            var current = last;
            var accumulated = 0.0;

            while (true) {

                last = current;
                current = System.currentTimeMillis();
                accumulated += current - last;

                while (accumulated > period) {
                    accumulated -= period;

                    showing = if (session.isEditSafe) {
                        max(0f, showing - ANIMATION_STEP)
                    } else {
                        1f
                    }
                    repaint()

                }

            }

        }

    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        val size = size
        g.stroke = BasicStroke(.2f)

//        val step = recording.sectionAt(session.cursor)

        synchronized(session.recording) {

            if (!session.recording.sections.isEmpty() && !session.recording.sections.last().timeSteps.isEmpty()) {


                val dePhased = session.recording.sections.last().timeSteps.last().dePhased
                val graph = Path2D.Double()

                val resolution = 1
                for (i in 0 until dePhased.size step resolution) {

                    val x = size.getWidth() * i / dePhased.size
                    if (i == 0) {

                        graph.moveTo(x, size.height / 2 + dePhased[i] * SCALE * showing)

                    } else {

                        graph.lineTo(x, size.height / 2 + dePhased[i] * SCALE * showing)

                    }

                }
                g.color = interpolateColour(g.color, Color.RED, showing)
                g.draw(graph)

            } else {

                g.draw(Line2D.Double(0.0, height / 2.0, width.toDouble(), height / 2.0))

            }

        }

    }

    companion object {

        private const val SCALE = 250.0

        private const val ANIMATION_TIME = .3
        private const val ANIMATION_REFRESH_RATE = 60
        private const val ANIMATION_STEP = (1 / ANIMATION_TIME / ANIMATION_REFRESH_RATE).toFloat()

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
