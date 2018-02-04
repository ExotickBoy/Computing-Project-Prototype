package components

import components.RecordingEditPane.Companion.overlap
import core.Session
import core.Session.Companion.DELETE_DISTANCE
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.transform.Transform
import kotlin.math.max
import kotlin.math.sign

internal class HistoryView internal constructor(private val session: Session) : Canvas(500.0, 300.0) {

    init {

        draw(graphicsContext2D)

        ScrollController(false, this, session)

        session.addOnUpdate {
            println("updaging")
            repaint()
        }
        session.addOnEdited { repaint() }

    }

    fun repaint() {
        graphicsContext2D.clearRect(0.0, 0.0, width, height)
    }

    private fun draw(g: GraphicsContext) {

        synchronized(session.recording) {

            g.lineWidth = 1.0
            g.stroke = Color.MAGENTA

            session.recording.sections.filterIndexed { index, _ ->
                index != session.swap
            }.forEachIndexed { index, it ->

                        val overlap = it.timeStepRange overlap session.visibleStepRange

                        for (x in overlap) {
//                          g.drawImage(it.timeSteps[x - it.timeStepStart].melImage, x - session.stepFrom, 0, 1, height)
                        }

                        if (index != 0)
                            g.strokeLine(it.timeStepStart - session.stepFrom + 0.5, 0.0, it.timeStepStart - session.stepFrom + 0.5, height)
                    }

            g.lineWidth = 2.0
            g.stroke = Color.RED
            g.strokeLine(session.onScreenStepCursor.toDouble(), 0.0, session.onScreenStepCursor.toDouble(), height)

            val swap = session.swap
            val swapWith = session.swapWith
            // I am making these local variables because making them final means that they are automatically cast as none null

            if (swap != null) {

                when {
                    session.swapWithSection == true -> {

                        val sectionTo = session.recording.sections[swapWith]
                        val from = sectionTo.timeStepStart - session.stepFrom.toDouble()

                        g.fill = Color(0.0, 1.0, 0.0, .5)
                        g.fillRect(from, 0.0, sectionTo.timeSteps.size.toDouble(), height)

                    }
                    session.swapWithSection == false -> {

                        val from: Double
                        from = if (swapWith == session.recording.sections.size) {
                            session.recording.sections.last().timeStepEnd
                        } else {
                            val sectionTo = session.recording.sections[swapWith]
                            sectionTo.timeStepStart - session.stepFrom
                        }.toDouble()

                        g.stroke = Color.GREEN
                        g.lineWidth = 2.0
                        g.strokeLine(from, 0.0, from, height)

                    }
                    else -> {
                        g.fill = Color(1.0, 0.0, 0.0, 0.5)
                        g.fillRect(0.0, 0.0, width, height * DELETE_DISTANCE / 2)
                    }
                }

                val section = session.recording.sections[swap]

                val transformBefore = g.transform
                val y = height * (max(-session.lastY / (2 * DELETE_DISTANCE) + 0.5, 0.0) * sign(session.lastY - 0.5) + 0.1)
                g.transform(Transform.affine(1.0, 0.0, 0.0, 0.8, 0.0, y))
                for (x in 0 until section.timeSteps.size) {

//                    g.drawImage(section.timeSteps[x].melImage, session.lastX + x, 0, 1, height, null)

                }

                g.transform = transformBefore

            }

        }

    }

}
