package components

import core.Session
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.JPanel

internal class PhaserPane internal constructor(private val session: Session) : JPanel() {

    init {

        preferredSize = Dimension(500, 150)

    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        val size = size
        g.stroke = BasicStroke(.2f)

        if (recording.timeSteps.size > 0 && !session.analyser.isPaused) {

            val dePhased = recording.timeSteps[recording.timeSteps.size - 1].dePhased
            val graph = Path2D.Double()

            val resolution = 1
            for (i in 0 until dePhased.size step resolution) {

                val x = size.getWidth() * i / dePhased.size
                if (i == 0) {

                    graph.moveTo(x, size.height / 2 + dePhased[i] * SCALE)

                } else {

                    graph.lineTo(x, size.height / 2 + dePhased[i] * SCALE)

                }

            }
            g.draw(graph)

        } else {

            g.draw(Line2D.Double(0.0, height / 2.0, width.toDouble(), height / 2.0))

        }

    }

    private val recording
        get() = session.recording

    companion object {

        private const val SCALE = 250.0

    }

}
