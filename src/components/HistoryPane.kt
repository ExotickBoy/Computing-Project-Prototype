package components

import core.Session
import java.awt.*
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class HistoryPane internal constructor(private val session: Session) : JPanel() {

    init {

        preferredSize = Dimension(500, 300)

        val scrollController = ScrollController(false, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)

    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        synchronized(session.recording) {

            session.recording.sections.filter {
                it.range overlaps session.visibleRange
            }.forEach {

                for (x in max(it.from, session.from)..min(it.correctedTo, session.to)) {

                    g.drawImage(session.recording.timeSteps[x].melImage, session.onScreenCursor + x - session.correctedCursor, 0, 1, height, null)

                }

            }

        }

        g.stroke = BasicStroke(2f)
        g.color = Color.RED
        g.draw(line(session.onScreenCursor, 0.0, session.onScreenCursor, height))

        g.stroke = BasicStroke(.75f)
        session.recording.sections.filter { it.correctedTo in session.visibleRange }.filter { it.to != -1 }
                .forEach {
                    g.color = Color.MAGENTA
                    g.draw(line(it.correctedTo - session.from, 0, it.correctedTo - session.from, height))
                }

    }

}
