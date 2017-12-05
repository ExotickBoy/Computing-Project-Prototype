package components

import core.Analyser
import core.Recording
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel

class HistoryPane internal constructor(private val analyser: Analyser) : JPanel() {

    init {

        preferredSize = Dimension(500, 300)

    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        synchronized(recording) {

            for (x in 0 until minOf(width, recording.timeSteps.size)) {

                g.drawImage(recording.timeSteps[recording.timeSteps.size - 1 - x].melImage, width - x, 0, 1, height, null)

            }

        }

    }

    private val recording: Recording
        get() = analyser.recording

}
