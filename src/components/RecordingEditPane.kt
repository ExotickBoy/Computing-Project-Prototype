package components

import core.AppInstance
import core.Session
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.geom.Line2D
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

public class RecordingEditPane(val session: Session) : AppInstance.ApplicationPane() {

    private val historyPane = HistoryPane(session)
    private val phaserPane = PhaserPane(session)
    private val networkOutputPane = NetworkOutputPane(session)
    private val noteOutputPane = NoteOutputPane(session)
    private val controlPane = ControlPane(session)

    override fun onCreate() {

        synchronized(session.recording) {

            val historyPanel = JPanel(GridLayout())
            historyPanel.border = BorderFactory.createEtchedBorder()
            historyPanel.add(historyPane)

            val phaserPanel = JPanel(GridLayout())
            phaserPanel.border = BorderFactory.createEtchedBorder()
            phaserPanel.add(phaserPane)

            val networkOutputPanel = JPanel(GridLayout())
            networkOutputPanel.border = BorderFactory.createEtchedBorder()
            networkOutputPanel.add(networkOutputPane)

            val noteOutputPanel = JPanel(GridLayout())
            noteOutputPanel.border = BorderFactory.createEtchedBorder()
            noteOutputPanel.add(noteOutputPane)

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

    override fun onPause() {

        AppInstance.title = core.FRAME_TITLE

    }

    override fun onResume() {

        AppInstance.title = "${core.FRAME_TITLE} - ${session.recording.name}"

    }

    override fun onDestroy() {

        session.end()
        phaserPane.end()

        if (session.recording.length != 0.0) {

            session.recording.save()

        }

    }

    companion object {

        fun line(x1: Number, y1: Number, x2: Number, y2: Number): Line2D.Double =
                Line2D.Double(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())

        infix fun IntRange.overlap(other: IntRange): IntRange = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)

        infix fun ClosedFloatingPointRange<Double>.overlap(other: ClosedFloatingPointRange<Double>) = max(this.start, other.start)..min(this.endInclusive, other.endInclusive)
        // These need to be two separate methods because the superclass both these types of range share, only store comparators
        // for if something is in range, thus you can't know the start and end values

    }

}