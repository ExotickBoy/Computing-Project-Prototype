package components

import core.Recording
import core.Session
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.geom.Line2D
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class HistoryPane internal constructor(private val session: Session) : JPanel(), MouseMotionListener, MouseListener {

    private var lastX: Int = 0

    init {

        preferredSize = Dimension(500, 300)
        addMouseMotionListener(this)
        addMouseListener(this)
    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        val cursor = if (session.cursor != -1) session.cursor else session.recording.length
        val onScreenCursor = min(max(width - (session.recording.length - cursor), width / 2), cursor)
        val from = max(cursor - onScreenCursor, 0)
        val to = min(cursor + (width - onScreenCursor), session.recording.length)

        synchronized(recording) {

            for (x in from until to) {
                g.drawImage(recording.timeSteps[x].melImage, onScreenCursor + x - cursor, 0, 1, height, null)
            }

        }
        g.color = Color.RED
        g.draw(Line2D.Double(onScreenCursor.toDouble(), 0.0, onScreenCursor.toDouble(), height.toDouble()))


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

    override fun mousePressed(e: MouseEvent) {

        lastX = e.x

    }

    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}

    override fun mouseClicked(e: MouseEvent) {}

    private val recording: Recording
        get() = session.recording

}
