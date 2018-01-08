package components

import core.Session
import core.noteString
import java.awt.*
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.geom.Rectangle2D
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class NoteOutputPane(private val session: Session) : JPanel(), ComponentListener {

    private var lineHeight: Double = 0.0
    private val margin: Int
    private val spacing: Float
    private val padding: Int

    init {

        preferredSize = Dimension(500, 140)

        val fontMetrics = getFontMetrics(font)

        margin = session.recording.tuning.strings
                .map { it.noteString }
                .map { fontMetrics.stringWidth(it) }
                .max() ?: 0

        spacing = ((0..session.recording.tuning.maxFret).map { fontMetrics.stringWidth(it.toString()) }.max() ?: 0) + 2.5f
        padding = 5

        val scrollController = ScrollController(true, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)
        addComponentListener(this)

    }

    override fun paintComponent(g2: Graphics) {

        val g: Graphics2D = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHints(rh)

        g.stroke = BasicStroke(.5f)

        synchronized(session.recording) {

            val stringHeaderOffset = min(max((session.noteWidth / 2 - session.onScreenNoteCursor) * spacing, 0.0), margin + 2.0 * padding)

            for (index in 0..session.recording.tuning.size) {
                g.color = when (index % 2) {
                    1 -> Color(232, 232, 232)
                    else -> Color(245, 245, 245)
                }
                g.fill(Rectangle2D.Double(0.0, index * lineHeight, width.toDouble(), lineHeight))
            }

            session.recording.sections.filter {
                it.noteRange.toDoubleRange() overlaps session.visibleNoteRange
            }.forEach {

                g.color = Color(86, 86, 86)
                it.noteRange.forEachIndexed { indexOfIndex, index ->
                    val placement = session.recording.placements[index]
                    g.drawString(placement.fret.toString(), (stringHeaderOffset).toFloat() + (it.noteRecordingStart - session.noteFrom + 0.5f + indexOfIndex).toFloat() * spacing - g.fontMetrics.stringWidth(placement.fret.toString()) / 2, (lineHeight * (placement.string + 2) - (lineHeight - g.font.size) / 2).toFloat())
                }
                if (it.correctedLength != 0 && it.recordingStart != 0) { // doesn't draw a separation at the beginning of if there are no notes

                    g.color = Color.MAGENTA
                    g.draw(line(
                            stringHeaderOffset + (it.noteRecordingStart - session.noteFrom) * spacing,
                            0,
                            stringHeaderOffset + (it.noteRecordingStart - session.noteFrom) * spacing,
                            height
                    ))

                }

            }

            session.recording.chordController.clear()
            session.recording.chordController.feed(session.recording.notes)

            session.recording.chordController.states.map { it.chord }.forEach {
                if (it != null)
                    g.drawString(it.asString(), (stringHeaderOffset).toFloat() + (it.noteStart - session.noteFrom + 0.5f).toFloat() * spacing - g.fontMetrics.stringWidth(it.asString()) / 2, (lineHeight * 1 - (lineHeight - g.font.size) / 2).toFloat())
            }

            for (index in 0..session.recording.tuning.size) {
                g.color = when (index % 2) {
                    1 -> Color(232, 232, 232)
                    else -> Color(245, 245, 245)
                }
                g.fill(Rectangle2D.Double(-(margin + 2 * padding) + stringHeaderOffset, index * lineHeight, margin + 2.0 * padding, lineHeight))
                g.color = Color(86, 86, 86)
                if (index != 0) {
                    g.drawString(session.recording.tuning[index - 1].noteString, (-(margin + 2 * padding) + stringHeaderOffset + padding).toFloat(), (lineHeight * (index + 1) - (lineHeight - g.font.size) / 2).toFloat())
                }
            }
            g.draw(line(stringHeaderOffset, 0, stringHeaderOffset, height.toDouble()))

            g.stroke = BasicStroke(2f)
            g.color = Color.RED

            g.draw(line(
                    stringHeaderOffset + session.onScreenNoteCursor * spacing,
                    0.0,
                    stringHeaderOffset + session.onScreenNoteCursor * spacing,
                    height
            ))

        }

    }

    override fun componentResized(e: ComponentEvent) {

        lineHeight = height / (session.recording.tuning.size + 1.0)

        session.width = e.component.width
        session.noteWidth = e.component.width.toDouble() / spacing

    }

    override fun componentMoved(e: ComponentEvent) {}

    override fun componentHidden(e: ComponentEvent) {}

    override fun componentShown(e: ComponentEvent) {}

    private fun IntRange.toDoubleRange(): ClosedFloatingPointRange<Double>
            = start.toDouble()..endInclusive.toDouble()

}

