package components

import core.Session
import java.awt.BorderLayout
import java.awt.GridLayout
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