package components

import core.Session
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.geom.Line2D
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class ContentsPane(session: Session) : JPanel() {

    init {

        synchronized(session.recording) {

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

    companion object {

        fun line(x1: Number, y1: Number, x2: Number, y2: Number): Line2D.Double =
                Line2D.Double(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        infix fun IntRange.overlap(other: IntRange): IntRange
                = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)

        infix fun ClosedFloatingPointRange<Double>.overlap(other: ClosedFloatingPointRange<Double>)
                = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)
        // These need to be two separate methods because the superclass both these types of range share, only store comparators
        // for if something is in range, thus you can't know the start and end values

    }

}