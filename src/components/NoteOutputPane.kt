package components

import core.Session
import core.noteString
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import javax.swing.JPanel

class NoteOutputPane(private val session: Session) : JPanel() {

    private var lastX: Int = 0

    init {

        preferredSize = Dimension(500, 150)

        val scrollController = ScrollController(true, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)


    }

    override fun paintComponent(g2: Graphics?) {

        val g: Graphics2D = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        g.stroke = BasicStroke(.5f)

        synchronized(session) {

            val recording = session.recording

            val h = 150.0 / recording.tuning.size
            val margin = recording.tuning.strings
                    .map { it.noteString }
                    .map { g.fontMetrics.stringWidth(it) }
                    .max() ?: 0

            for (index in 0 until recording.tuning.size) {

                g.color = when (index % 2) {
                    1 -> Color(232, 232, 232)
                    else -> Color(245, 245, 245)
                }
                g.fill(Rectangle2D.Double(0.0, index * h, width.toDouble(), h))

                g.color = Color(86, 86, 86)
                g.drawString(recording.tuning[index].noteString, 5F, (h * (index + 1) - (h - g.font.size) / 2).toFloat())
            }
            g.draw(Line2D.Double(margin.toDouble() + 10, 0.0, margin.toDouble() + 10, height.toDouble()))

            val spacing = ((0..25).map { g.fontMetrics.stringWidth(it.toString()) }.max() ?: 0) + 2.5f

            recording.placements.forEachIndexed { i, placement ->

                g.drawString(placement.fret.toString(), 15f + margin + (i + .5f) * spacing - g.fontMetrics.stringWidth(placement.fret.toString()) / 2, (h * (placement.string + 1) - (h - g.font.size) / 2).toFloat())

            }

        }


    }

}