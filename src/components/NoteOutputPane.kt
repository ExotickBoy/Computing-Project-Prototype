package components

import components.RecordingEditPane.Companion.line
import components.RecordingEditPane.Companion.overlap
import core.Model
import core.Note.Companion.noteStringShort
import core.Section
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
    private val padding: Int
    private val headerHeight: Int

    val spacing: Float

    private val fontMetrics = getFontMetrics(font)

    init {

        margin = session.recording.tuning.strings
                .map { it.noteStringShort }
                .map { fontMetrics.stringWidth(it) }
                .max() ?: 0

        spacing = ((session.recording.tuning.capo..session.recording.tuning.maxFret).map {
            fontMetrics.stringWidth(it.toString())
        }.max() ?: 0) + 2.5f
        headerHeight = (Model.START_PITCH..Model.END_PITCH).flatMap {
            return@flatMap Section.patters.map { chordPattern ->
                return@map "${it.noteStringShort}${chordPattern.suffix}"
            }
        }.map { fontMetrics.stringWidth(it) }.max() ?: 0
        padding = 5

        val scrollController = ScrollController(true, this, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)
        addComponentListener(this)

        session.addOnUpdate { repaint() }
        session.addOnEdited { repaint() }

        preferredSize = Dimension(500, session.recording.tuning.size * PREFERRED_LINE_HEIGHT + headerHeight)

    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g: Graphics2D = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
        g.setRenderingHints(rh)

        g.stroke = BasicStroke(.5f)

        synchronized(session.recording) {

            val stringHeaderOffset = min(max((session.clusterWidth / 2 - session.onScreenClusterCursor) * spacing, 0.0), margin + 2.0 * padding)

            // stripes
            g.color = STRIPE_LIGHT
            g.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), headerHeight.toDouble()))
            (0 until session.recording.tuning.size).forEach { index ->
                g.color = if (index % 2 == 1) STRIPE_LIGHT else STRIPE_DARK
                g.fill(Rectangle2D.Double(0.0, index * lineHeight + headerHeight, width.toDouble(), lineHeight))
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
                                    placement.fret.toString(),
                                    (stringHeaderOffset).toFloat() + (it.clusterStart - session.clusterFrom + 0.5f + index).toFloat() * spacing - g.fontMetrics.stringWidth(placement.fret.toString()) / 2,
                                    (lineHeight * (placement.string + 1) - (lineHeight - g.font.size) / 2 + headerHeight).toFloat()
                            )

                        }

                        val transformBefore = g.transform
                        g.transform(AffineTransform( // rotation through 90 degrees
                                0.0,
                                -1.0,
                                1.0,
                                0.0,
                                stringHeaderOffset + (it.clusterStart - session.clusterFrom + index) * spacing,
                                headerHeight.toDouble()
                        ))

                        g.color = if (cluster.boldHeading) Color(86, 86, 86) else Color(150, 150, 150)
                        g.drawString(
                                cluster.heading, (headerHeight - fontMetrics.stringWidth(cluster.heading)) / 2, (font.size + (spacing - font.size) / 2).toInt()
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

            g.color = STRIPE_LIGHT
            g.fill(Rectangle2D.Double(-(margin + 2 * padding) + stringHeaderOffset, 0.0, margin + 2.0 * padding, headerHeight.toDouble()))
            for (index in 0 until session.recording.tuning.size) {
                g.color = if (index % 2 == 1) STRIPE_LIGHT else STRIPE_DARK
                g.color = Color(86, 86, 86)

                g.drawString(
                        (session.recording.tuning[index] + session.recording.tuning.capo).noteStringShort,
                        (-(margin + 2 * padding) + stringHeaderOffset + padding).toFloat(),
                        (lineHeight * (index + 1) + headerHeight - (lineHeight - g.font.size) / 2).toFloat()
                )

            }
            g.draw(line(stringHeaderOffset, headerHeight, stringHeaderOffset, height.toDouble()))

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

            lineHeight = (height - headerHeight) / session.recording.tuning.size.toDouble()

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
        private val STRIPE_LIGHT = Color(232, 232, 232)
        private val STRIPE_DARK = Color(245, 245, 245)

    }

}