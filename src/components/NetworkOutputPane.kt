package components

import core.Model
import core.Session
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class NetworkOutputPane(private val session: Session) : JPanel() {

    init {

        preferredSize = Dimension(500, 100)

        val scrollController = ScrollController(false, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)

    }

    override fun paintComponent(g2: Graphics?) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        synchronized(session.recording) {

            g.stroke = BasicStroke(1f)
            g.color = Color.MAGENTA
            (0 until session.recording.sections.size).filter { it != session.swap }.map { session.recording.sections[it] }.filter {
                it.recordingRange overlaps session.visibleRange
            }.forEachIndexed { index, it ->

                for (x in max(0, session.from - it.recordingStart) until min(it.correctedLength, session.to - it.recordingStart)) {

                    g.drawImage(session.recording.timeSteps[x + it.timeStepStart].noteImage, x + it.recordingStart - session.from, 0, 1, Model.PITCH_RANGE, null)

                }

                if (index != 0)
                    g.draw(line(it.recordingStart - session.from + 0.5, 0, it.recordingStart - session.from + 0.5, Model.PITCH_RANGE))


            }

            g.stroke = BasicStroke(2f)
            g.color = Color.RED
            g.draw(line(session.onScreenCursor, 0.0, session.onScreenCursor, Model.PITCH_RANGE))

            val swap = session.swap
            val swapWith = session.swapWith
            // I am making these local variables because making them final means that they are automatically cast as none null

            if (swap != null) {

                if (session.swapWithSection) {

                    val sectionTo = session.recording.sections[swapWith]
                    val from = sectionTo.recordingStart - session.from.toDouble()

                    g.color = Color(0f, 1f, 0f, .5f)
                    g.fill(Rectangle2D.Double(from, 0.0, min(sectionTo.correctedLength.toDouble() + 1, width - from), Model.PITCH_RANGE.toDouble()))

                } else {

                    val from: Double
                    from = if (swapWith == session.recording.sections.size) {
                        session.recording.timeSteps.size
                    } else {
                        val sectionTo = session.recording.sections[swapWith]
                        sectionTo.recordingStart - session.from
                    }.toDouble()

                    g.color = Color(0f, 1f, 0f, 1f)
                    g.stroke = BasicStroke(2f)
                    g.draw(line(from, 0, from, Model.PITCH_RANGE))

                }

                val section = session.recording.sections[swap]

                val transformBefore = g.transform
                g.transform(AffineTransform(1.0, 0.0, 0.0, 0.8, 0.0, Model.PITCH_RANGE * .1))
                for (x in 0 until min(section.correctedLength, width - session.lastX)) {

                    g.drawImage(session.recording.timeSteps[section.timeStepStart + x].noteImage, session.lastX + x, 0, 1, Model.PITCH_RANGE, null)

                }

                g.transform = transformBefore

            }

        }

    }

}