package components

import core.Session
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

class ContentsPane(session: Session) : JPanel() {

    init {

        val historyPane = HistoryPane(analyser)
        val historyPanel = JPanel(GridLayout())
        historyPanel.border = BorderFactory.createEtchedBorder()
        historyPanel.add(historyPane)

        val phaserPane = PhaserPane(analyser.recording)
        val phaserPanel = JPanel(GridLayout())
        phaserPanel.border = BorderFactory.createEtchedBorder()
        phaserPanel.add(phaserPane)

        val outputPane = OutputPane(analyser)
        val outputPanel = JPanel(GridLayout())
        outputPanel.border = BorderFactory.createEtchedBorder()
        outputPanel.add(outputPane)

        val controlPane = ControlPane(analyser)
        val controlPanel = JPanel(GridLayout())
        controlPanel.border = BorderFactory.createEtchedBorder()
        controlPanel.add(controlPane)

        val topPanel = JPanel(BorderLayout())
        topPanel.add(phaserPanel, BorderLayout.NORTH)
        topPanel.add(historyPanel, BorderLayout.CENTER)
        topPanel.add(outputPanel, BorderLayout.SOUTH)

        layout = BorderLayout()
        add(topPanel, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.SOUTH)

    }

}