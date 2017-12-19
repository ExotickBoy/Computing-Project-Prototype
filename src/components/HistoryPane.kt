package components

import core.Recording
import core.Session
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
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

        val cursor = if (session.cursor != -1) session.cursor else session.recording.length
        val onScreenCursor = min(max(width - (session.recording.length - cursor), width / 2), cursor)
        val from = max(cursor - onScreenCursor, 0)
        val to = min(cursor + (width - onScreenCursor), session.recording.length)

        synchronized(recording) {
            if (session.swap != null) {

                session.recording.sections.filter {
                    to in it.from..it.correctedTo || from in it.from..it.correctedTo || it.from in from..to || it.to in from..to
                }.forEach {

                    val start = max(it.from, from)
                    val length = min(it.correctedTo, to) - start

                    val transformBefore = g.transform
                    g.transform(AffineTransform(
                            1 - (Session.swapModeZoom * 2 / it.length),
                            0.0,
                            0.0,
                            1 - (Session.swapModeZoom * 2 / height),
                            Session.swapModeZoom + onScreenCursor - cursor + start,
                            Session.swapModeZoom
                    ))

                    for (x in 0..length) {

                        g.drawImage(recording.timeSteps[start + x].melImage, x, 0, 1, height, null)

                    } // TODO fix this

                    g.transform = transformBefore

                }

            } else {

                session.recording.sections.filter {
                    to in it.from..it.correctedTo || from in it.from..it.correctedTo || it.from in from..to || it.to in from..to
                }.forEach {

                    for (x in max(it.from, from)..min(it.correctedTo, to)) {

                        g.drawImage(recording.timeSteps[x].melImage, onScreenCursor + x - cursor, 0, 1, height, null)

                    }

                }

            }

        }

        g.stroke = BasicStroke(2f)
        g.color = Color.RED
        g.draw(Line2D.Double(onScreenCursor.toDouble(), 0.0, onScreenCursor.toDouble(), height.toDouble()))


        g.stroke = BasicStroke(.75f)
        session.recording.sections.filter { it.correctedTo in from..to }.filter { it.to != -1 }
                .forEach {
                    g.color = Color.MAGENTA
                    g.draw(Line2D.Double((it.correctedTo - from).toDouble(), 0.0, (it.correctedTo - from).toDouble(), height.toDouble()))
                }

    }

    private val recording: Recording
        get() = session.recording

}
