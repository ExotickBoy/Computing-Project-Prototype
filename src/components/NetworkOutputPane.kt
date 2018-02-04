package components

import components.RecordingEditPane.Companion.line
import components.RecordingEditPane.Companion.overlap
import core.Model
import core.Session
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import javax.swing.JPanel

internal class NetworkOutputPane(private val session: Session) : JPanel() {

    init {

        preferredSize = Dimension(500, Model.PITCH_RANGE)

//        ScrollController(false, this, session)

        session.addOnUpdate { repaint() }
        session.addOnEdited { repaint() }

    }

    override fun paintComponent(g2: Graphics?) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
        g.setRenderingHints(rh)

        synchronized(session.recording) {

            g.stroke = BasicStroke(1f)
            g.color = Color.MAGENTA

            session.recording.sections.filterIndexed { index, _ ->
                index != session.swap
            }.forEachIndexed { index, it ->

                val overlap = it.timeStepRange overlap session.visibleStepRange

                for (x in overlap) {

                    g.drawImage(it.timeSteps[x - it.timeStepStart].noteImage, x - session.stepFrom, 0, 1, height, null)

                }

                if (index != 0)
                    g.draw(line(it.timeStepStart - session.stepFrom + 0.5, 0, it.timeStepStart - session.stepFrom + 0.5, height))

            }

            session.recording.sections.forEach { section ->
                section.clusters.forEach {
                    g.color = Color.RED
                    g.draw(line(
                            it.relTimeStepStart + section.timeStepStart - session.stepFrom,
                            0,
                            it.relTimeStepStart + section.timeStepStart - session.stepFrom,
                            height
                    ))
                }
            }

            g.stroke = BasicStroke(2f)
            g.color = Color.RED
            g.draw(line(session.onScreenStepCursor, 0.0, session.onScreenStepCursor, height))

            val swap = session.swap
            val swapWith = session.swapWith
            // I am making these local variables because making them final means that they are automatically cast as none null

            if (swap != null) {

                if (session.swapWithSection == true) {

                    val sectionTo = session.recording.sections[swapWith]
                    val from = sectionTo.timeStepStart - session.stepFrom.toDouble()

                    g.color = Color(0f, 1f, 0f, .5f)
                    g.fill(Rectangle2D.Double(from, 0.0, sectionTo.timeSteps.size.toDouble(), height.toDouble()))

                } else if (session.swapWithSection == false) {

                    val from: Double
                    from = if (swapWith == session.recording.sections.size) {
                        session.recording.sections.last().timeStepEnd
                    } else {
                        val sectionTo = session.recording.sections[swapWith]
                        sectionTo.timeStepStart - session.stepFrom
                    }.toDouble()

                    g.color = Color(0f, 1f, 0f, 1f)
                    g.stroke = BasicStroke(2f)
                    g.draw(line(from, 0, from, height))

                }

                val section = session.recording.sections[swap]

                val transformBefore = g.transform
                g.transform(AffineTransform(1.0, 0.0, 0.0, 0.8, 0.0, height * .1))
                for (x in 0 until section.timeSteps.size) {

                    g.drawImage(section.timeSteps[x].noteImage, session.lastX + x, 0, 1, height, null)

                }

                g.transform = transformBefore

            }

        }

    }

}