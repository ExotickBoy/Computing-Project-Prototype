package components

import core.Session
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.geom.Line2D
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel

class ContentsPane(session: Session) : JPanel() {

    init {

        session.addOnUpdateListener {
            repaint()
        }

        val historyPane = HistoryPane(session)
        val historyPanel = JPanel(GridLayout())
        historyPanel.border = BorderFactory.createEtchedBorder()
        historyPanel.add(historyPane)

        val phaserPane = PhaserPane(session)
        val phaserPanel = JPanel(GridLayout())
        phaserPanel.border = BorderFactory.createEtchedBorder()
        phaserPanel.add(phaserPane)

        val networkOutputPane = NetworkOutputPane(session)
        val networkOutputPanel = JPanel(GridLayout())
        networkOutputPanel.border = BorderFactory.createEtchedBorder()
        networkOutputPanel.add(networkOutputPane)

        val noteOutputPane = NoteOutputPane(session)
        val noteOutputPanel = JPanel(GridLayout())
        noteOutputPanel.border = BorderFactory.createEtchedBorder()
        noteOutputPanel.add(noteOutputPane)

        val controlPane = ControlPane(session)
        val controlPanel = JPanel(GridLayout())
        controlPanel.border = BorderFactory.createEtchedBorder()
        controlPanel.add(controlPane)

        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        topPanel.add(phaserPanel)
        topPanel.add(historyPanel)
        topPanel.add(networkOutputPanel)
        topPanel.add(noteOutputPanel)

        layout = BorderLayout()
        add(topPanel, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.SOUTH)

    }

}

fun line(x1: Number, y1: Number, x2: Number, y2: Number): Line2D.Double =
        Line2D.Double(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

infix fun IntRange.overlaps(other: IntRange): Boolean
        = first in other || endInclusive in other || other.first in this || other.endInclusive in this