package components

import core.Model
import core.Session
import java.awt.*
import java.awt.geom.Line2D
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class NetworkOutputPane(private val session: Session) : JPanel() {

    private var lastX: Int = 0

    init {

        preferredSize = Dimension(500, 100)

        val scrollController = ScrollController(false, session)
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

            val onScreenCursor = min(max(width - (session.recording.length - session.correctedCursor), width / 2), session.correctedCursor)
            val from = max(session.correctedCursor - onScreenCursor, 0)
            val to = min(session.correctedCursor + (width - onScreenCursor), session.recording.length)

            synchronized(recording) {

                for (x in from until to) {
                    g.drawImage(recording.timeSteps[x].noteImage, onScreenCursor + x - session.correctedCursor, 0, 1, Model.PITCH_RANGE, null)
                }

            }
            g.stroke = BasicStroke(2f)
            g.color = Color.RED
            g.draw(Line2D.Double(onScreenCursor.toDouble(), 0.0, onScreenCursor.toDouble(), Model.PITCH_RANGE.toDouble()))

            g.stroke = BasicStroke(.75f)
            session.recording.sections.filter { it.correctedTo in from..to }.filter { it.to != null }
                    .forEach {
                        g.color = Color.MAGENTA
                        g.draw(Line2D.Double((it.correctedTo - from).toDouble(), 0.0, (it.correctedTo - from).toDouble(), Model.PITCH_RANGE.toDouble()))
                    }

        }

    }

}