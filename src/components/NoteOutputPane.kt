package components

import components.RecordingEditPane.Companion.line
import components.RecordingEditPane.Companion.overlap
import core.Note.Companion.noteStringShort
import core.Session
import java.awt.*
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import javax.swing.JPanel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal class NoteOutputPane(private val session: Session) : JPanel(), ComponentListener {

    private var lineHeight: Double = 0.0
    private val margin: Int
    private val spacing: Float
    private val padding: Int

    init {

        preferredSize = Dimension(500, (session.recording.tuning.size + 1) * PREFERRED_LINE_HEIGHT)

        val fontMetrics = getFontMetrics(font)

        margin = session.recording.tuning.strings
                .map { it.noteStringShort }
                .map { fontMetrics.stringWidth(it) }
                .max() ?: 0

        spacing = ((session.recording.tuning.capo..session.recording.tuning.maxFret).map {
            fontMetrics.stringWidth(it.toString())
        }.max() ?: 0) + 2.5f

        padding = 5

        val scrollController = ScrollController(true, this, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)
        addComponentListener(this)

        session.addOnCursorChange { repaint() }
        session.addOnStepChange { repaint() }

    }

    override fun paintComponent(g2: Graphics) {

        val g: Graphics2D = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
        g.setRenderingHints(rh)

        g.stroke = BasicStroke(.5f)

        synchronized(session.recording) {

            val stringHeaderOffset = min(max((session.clusterWidth / 2 - session.onScreenClusterCursor) * spacing, 0.0), margin + 2.0 * padding)

            // stripes
            for (index in 0..session.recording.tuning.size) {
                g.color = when (index % 2) {
                    1 -> Color(232, 232, 232)
                    else -> Color(245, 245, 245)
                }
                g.fill(Rectangle2D.Double(0.0, index * lineHeight, width.toDouble(), lineHeight))
            }

            // sections and their notes

            session.recording.sections.forEach {

                val doubleRange = it.clusterRange.toDoubleRange() overlap session.visibleClusterRange
                val clusterRange = floor(doubleRange.start).toInt()..ceil(doubleRange.endInclusive).toInt()

                g.color = Color(86, 86, 86)
                it.clusters.forEachIndexed { index, cluster ->

                    if (index + it.clusterStart in clusterRange) {
                        g.color = Color(86, 86, 86)
                        cluster.placements.forEach { placement ->

                            g.drawString(
                                    placement.correctedFret.toString(),
                                    (stringHeaderOffset).toFloat() + (it.clusterStart - session.clusterFrom + 0.5f + index).toFloat() * spacing - g.fontMetrics.stringWidth(placement.fret.toString()) / 2,
                                    (lineHeight * (placement.string + 2) - (lineHeight - g.font.size) / 2).toFloat()
                            )

                        }

                        val transformBefore = g.transform
                        g.transform(AffineTransform(
                                0.0,
                                -1.0,
                                1.0,
                                0.0,
                                stringHeaderOffset + (it.clusterStart - session.clusterFrom + 0.5f + index) * spacing - g.fontMetrics.stringWidth(cluster.heading) / 2,
                                lineHeight * 1 - (lineHeight - g.font.size) / 2
                        ))

                        g.color = if (cluster.boldHeading) Color(86, 86, 86) else Color(150, 150, 150)
                        g.drawString(
                                cluster.heading, 0, font.size
//                                (stringHeaderOffset).toFloat() + (it.clusterStart - session.clusterFrom + 0.5f + index).toFloat() * spacing - g.fontMetrics.stringWidth(cluster.heading) / 2,
//                                (lineHeight * 1 - (lineHeight - g.font.size) / 2).toFloat()
                        )
                        g.transform = transformBefore //making sure that I reset the transformation I set so that the rest of the ui doesn't renter incorrectly

                    }
                }

                if (it.timeSteps.size != 0 && it.timeStepStart != 0) { // doesn't draw a separation at the beginning of if there are no notes

                    g.color = Color.MAGENTA
                    g.draw(line(
                            stringHeaderOffset + (it.clusterStart - session.clusterFrom) * spacing,
                            0,
                            stringHeaderOffset + (it.clusterStart - session.clusterFrom) * spacing,
                            height
                    ))

                }

            }

            // Tuning header

            for (index in 1..session.recording.tuning.size) {
                g.color = when (index % 2) {
                    1 -> Color(232, 232, 232)
                    else -> Color(245, 245, 245)
                }
                g.fill(Rectangle2D.Double(-(margin + 2 * padding) + stringHeaderOffset, index * lineHeight, margin + 2.0 * padding, lineHeight))
                g.color = Color(86, 86, 86)
                if (index != 0) {
                    g.drawString(
                            (session.recording.tuning[index - 1] + session.recording.tuning.capo).noteStringShort,
                            (-(margin + 2 * padding) + stringHeaderOffset + padding).toFloat(),
                            (lineHeight * (index + 1) - (lineHeight - g.font.size) / 2).toFloat()
                    )
                }
            }
            g.draw(line(stringHeaderOffset, lineHeight, stringHeaderOffset, height.toDouble()))

            // Cursor

            g.stroke = BasicStroke(2f)
            g.color = Color.RED

            g.draw(line(
                    stringHeaderOffset + session.onScreenClusterCursor * spacing,
                    0.0,
                    stringHeaderOffset + session.onScreenClusterCursor * spacing,
                    height
            ))

        }

    }

    override fun componentResized(e: ComponentEvent) {

        synchronized(session.recording) {

            lineHeight = height / (session.recording.tuning.size + 1.0)

            session.width = e.component.width
            session.clusterWidth = e.component.width.toDouble() / spacing

        }

    }

    override fun componentMoved(e: ComponentEvent) {}

    override fun componentHidden(e: ComponentEvent) {}

    override fun componentShown(e: ComponentEvent) {}

    private fun IntRange.toDoubleRange(): ClosedFloatingPointRange<Double> = start.toDouble()..endInclusive.toDouble()


    companion object {

        private const val PREFERRED_LINE_HEIGHT = 20

    }

}