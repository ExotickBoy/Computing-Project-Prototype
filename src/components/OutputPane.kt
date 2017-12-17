package components

import core.Model
import core.Session
import core.noteString
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class OutputPane(private val session: Session) : JPanel(), MouseListener, MouseMotionListener {

    private var lastX: Int = 0

    init {

        preferredSize = Dimension(500, 300)
        addMouseListener(this)

    }

    override fun paintComponent(g2: Graphics?) {

        val g: Graphics2D = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        g.stroke = BasicStroke(.5f)


        synchronized(session) {

            val recording = session.recording
            val analyser = session.analyser

            val cursor = if (session.cursor != -1) session.cursor else session.recording.length
            val onScreenCursor = min(max(width - (session.recording.length - cursor), width / 2), cursor)
            val from = max(cursor - onScreenCursor, 0)
            val to = min(cursor + (width - onScreenCursor), session.recording.length)

            synchronized(recording) {

                for (x in from until to) {
                    g.drawImage(recording.timeSteps[x].noteImage, onScreenCursor + x - cursor, 0, 1, Model.PITCH_RANGE, null)
                }

            }
            g.color = Color.RED
            g.draw(Line2D.Double(onScreenCursor.toDouble(), 0.0, onScreenCursor.toDouble(), Model.PITCH_RANGE.toDouble()))

            recording.timeSteps
                    .flatMap { it.notes }
                    .filter { it.pitch in recording.tuning }
                    .distinct()
                    .filter { it.end >= analyser.stepsShown - width }.forEach {

                g.draw(Line2D.Double((width - analyser.stepsShown + it.start).toDouble(), Model.PITCH_RANGE + 10.0, (width - analyser.stepsShown + it.end).toDouble(), Model.PITCH_RANGE + 10.0))

                val transformBefore = g.transform

                g.transform(AffineTransform(0.0, 1.0, -1.0, 0.0, width - analyser.stepsShown + it.start + (it.duration - g.font.size) / 2.0, Model.PITCH_RANGE + 20.0))
                g.drawString(it.pitch.noteString, 0, 0)

                g.transform = transformBefore

            }

            g.translate(0, 150)

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

    override fun mouseMoved(e: MouseEvent) {}
    override fun mouseDragged(e: MouseEvent) {

        if (!session.analyser.isRunning) {

            val dx = e.x - lastX
            lastX = e.x

            if (session.cursor == -1) {
                session.cursor = session.recording.length - 1
            }

            session.cursor = max(min(session.cursor - dx, session.recording.length), 0)

        }

    }


    override fun mouseClicked(e: MouseEvent) {


    }

    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {

        lastX = e.x

    }

}