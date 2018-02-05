package components

import components.RecordingEditPane.Companion.line
import core.Section
import core.Session
import core.Session.Companion.DELETE_DISTANCE
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.sign

internal class HistoryPane(
        private val session: Session,
        preferredHeight: Int,
        private val images: (Section) -> (List<BufferedImage>)
) : JPanel() {

    private val scrollController: ScrollController

    init {

        preferredSize = Dimension(500, preferredHeight)

        scrollController = ScrollController(false, this, session)
        addMouseMotionListener(scrollController)
        addMouseListener(scrollController)

        session.addOnUpdate { repaint() }
        session.addOnEdited { repaint() }

    }

    override fun paintComponent(g2: Graphics) {

        super.paintComponent(g2)

        val g = g2 as Graphics2D
        val rh = RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        rh[RenderingHints.KEY_ANTIALIASING] = RenderingHints.VALUE_ANTIALIAS_ON
        g.setRenderingHints(rh)

        synchronized(session.recording) {

            g.stroke = BasicStroke(1f)
            g.color = Color.MAGENTA
            session.recording.sections.filterIndexed { index, _ ->
                index != session.swap
            }.forEach { section ->

                        // I have found that trying to draw an image off screen has a negligible effect on performance
                        // which is why I don't check if all of these need to be drawn
                        images.invoke(section).fold(0) { acc, image ->

                            g.drawImage(image, acc + section.timeStepStart - session.stepFrom, 0, image.width, height, null)

                            acc + image.width

                        }
                        /*
                            for (i in 0..10000)
                                g.drawImage(image, -1000, 0, image.width, height, null)

                            This is the aforementioned test, I didn't measure how much longer this takes, but after
                            running this code it was apparent that there was no performance decrease even in this
                            extreme case
                         */
                        if (section.clusterStart != 0)
                            g.draw(line(section.timeStepStart - session.stepFrom + 0.5, 0, section.timeStepStart - session.stepFrom + 0.5, height))

                    }

            g.stroke = BasicStroke(2f)
            g.color = Color.RED
            g.draw(line(session.onScreenStepCursor, 0.0, session.onScreenStepCursor, height))

            val swap = session.swap
            val swapWith = session.swapWith
            // I am making these local variables because making them final means that they are automatically cast as none null

            if (swap != null) {

                when {
                    session.swapWithSection == true -> {

                        val sectionTo = session.recording.sections[swapWith]
                        val from = sectionTo.timeStepStart - session.stepFrom.toDouble()

                        g.color = Color(0f, 1f, 0f, .5f)
                        g.fill(Rectangle2D.Double(from, 0.0, sectionTo.timeStepLength.toDouble(), height.toDouble()))

                    }
                    session.swapWithSection == false -> {

                        val from: Double
                        from = if (swapWith == session.recording.sections.size) {
                            session.recording.sections.last().timeStepEnd
                        } else {
                            val sectionTo = session.recording.sections[swapWith]
                            sectionTo.timeStepStart - session.stepFrom
                        }.toDouble()

                        g.color = Color(0f, 1f, 0f, 1f)
                        g.stroke = BasicStroke(2f)
                        g.draw(line(from, 0, from, height))

                    }
                    else -> {
                        g.color = Color(1f, 0f, 0f, .5f)
                        g.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height * DELETE_DISTANCE / 2))
                    }
                }

                val section = session.recording.sections[swap]

                val transformBefore = g.transform
                val y = height * (max(-session.lastY / (2 * DELETE_DISTANCE) + 0.5, 0.0) * sign(session.lastY - 0.5) + 0.1)
                g.transform(AffineTransform(1.0, 0.0, 0.0, 0.8, 0.0, y))

                g.drawImage(images.invoke(section)[0], session.lastX + x, 0, images.invoke(section)[0].width, height, null)
                // melImages should only consist of one image at this stage since it must be processed for a step to be allowed

                g.transform = transformBefore

            }

        }

    }

}
