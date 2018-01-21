package components

import components.RecordingEditPane.Companion.line
import components.RecordingEditPane.Companion.overlap
import core.Session
import core.Session.Companion.DELETE_DISTANCE
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.sign

class HistoryPane internal constructor(private val session: Session) : JPanel() {

    private val scrollController: ScrollController

    init {

        preferredSize = Dimension(500, 300)

        scrollController = ScrollController(false, this, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)

        session.addOnCursorChange { repaint() }
        session.addOnSwapChange { repaint() }
        session.addOnStepChange { repaint() }

    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        synchronized(session.recording) {

            g.stroke = BasicStroke(1f)
            g.color = Color.MAGENTA
            session.recording.sections.filterIndexed { index, _ ->
                index != session.swap
            }.forEachIndexed { index, it ->

                val overlap = it.timeStepRange overlap session.visibleStepRange

                for (x in overlap) {

                    g.drawImage(it.timeSteps[x - it.timeStepStart].melImage, x - session.stepFrom, 0, 1, height, null)

                }

                if (index != 0)
                    g.draw(line(it.timeStepStart - session.stepFrom + 0.5, 0, it.timeStepStart - session.stepFrom + 0.5, height))

            }


            g.stroke = BasicStroke(2f)
            g.color = Color.RED
            g.draw(line(session.onScreenStepCursor, 0.0, session.onScreenStepCursor, height))

            val swap = session.swap
            val swapWith = session.swapWith
            // I am making these local variables because making them final means that they are automatically cast as none null

            if (swap != null) {

                when {
                    session.swapWithSection == true -> {

                        val sectionTo = session.recording.sections[swapWith]
                        val from = sectionTo.timeStepStart - session.stepFrom.toDouble()

                        g.color = Color(0f, 1f, 0f, .5f)
                        g.fill(Rectangle2D.Double(from, 0.0, sectionTo.timeSteps.size.toDouble(), height.toDouble()))

                    }
                    session.swapWithSection == false -> {

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
                    else -> {
                        g.color = Color(1f, 0f, 0f, .5f)
                        g.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height * DELETE_DISTANCE / 2))
                    }
                }

                val section = session.recording.sections[swap]

                val transformBefore = g.transform
                val y = height * (max(-session.lastY / (2 * DELETE_DISTANCE) + 0.5, 0.0) * sign(session.lastY - 0.5) + 0.1)
                g.transform(AffineTransform(1.0, 0.0, 0.0, 0.8, 0.0, y))
                for (x in 0 until section.timeSteps.size) {

                    g.drawImage(section.timeSteps[x].melImage, session.lastX + x, 0, 1, height, null)

                }

                g.transform = transformBefore

            }

        }

    }

}
