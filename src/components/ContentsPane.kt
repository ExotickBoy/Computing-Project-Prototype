package components

import core.Analyser
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

class ContentsPane(analyser: Analyser) : JPanel() {

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

        layout = BorderLayout()
        add(phaserPanel, BorderLayout.NORTH)
        add(historyPanel, BorderLayout.CENTER)
        add(outputPanel, BorderLayout.SOUTH)

    }

}