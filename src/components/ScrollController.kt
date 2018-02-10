package components

import core.Session
import javafx.animation.PauseTransition
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.util.Duration
import kotlin.math.*


/**
 * This class is the mouse listener that is reused int he various output panes.
 * It controls scrolling of the cursor and swapping of sections
 * @property isNote If the scrolling should wrap to the closest note
 * @property session The session that this controller is responsible for
 */
internal class ScrollController(private val isNote: Boolean, private val owner: Canvas, private val session: Session) {

    private var draggingThread: DraggingThread? = null

    init {

        owner.setOnMouseDragged {

            val dx = it.x - session.lastX

            synchronized(session.recording) {

                if (session.state == Session.SessionState.EDIT_SAFE && dx != 0.0) {
                    // im using != true, because it cold be null

                    if (!isNote) {
                        session.stepCursor = max(min(session.correctedStepCursor - dx, session.recording.timeStepLength.toDouble()), 0.0).roundToInt()
                    } else {
                        session.clusterCursor = session.correctedClusterCursor - session.clusterWidth * dx / session.width
                    }
                    it.consume()

                }
                session.lastX = it.x.roundToInt()
                session.lastY = it.y / owner.height
            }


        }

        owner.setOnMousePressed {

            session.lastX = it.x.roundToInt()
            session.lastY = it.y / owner.height

        }

        owner.setOnMouseReleased {
            draggingThread?.interrupt()
        }

        owner.setOnMouseClicked {

            if (!isNote && it.isPrimaryButtonDown && it.isStillSincePress) {
                session.stepCursor = (session.stepFrom + it.x).roundToInt()
                it.consume()
            }

        }

        val holdTimer = PauseTransition(Duration(holdTime))
        holdTimer.setOnFinished {
            if (session.state == Session.SessionState.EDIT_SAFE && !isNote) {
                synchronized(session.recording) {

                    session.swap = session.recording.sectionAt(session.lastX + session.correctedStepCursor
                            - session.onScreenStepCursor
                    )
                    session.updateSwapWith()
                    draggingThread = DraggingThread(this)

                }
            }
        }

        owner.addEventHandler(MouseEvent.MOUSE_PRESSED, { holdTimer.playFromStart() })
        owner.addEventHandler(MouseEvent.MOUSE_RELEASED) { holdTimer.stop() }
        owner.addEventHandler(MouseEvent.DRAG_DETECTED) { holdTimer.stop() }

    }

    private fun movementDirection(x: Int): Int {

        return ((1.025.pow(max(abs(x - session.width / 2.0) - session.width / 5.0, 0.0)) - 1) * sign(x - session.width / 2.0)).toInt()

    }

    companion object {

        const val holdTime: Double = .3 * 1000

    }

    private class DraggingThread(private val controller: ScrollController) : Thread() {

        init {
            name = "Drag Thread"
            start()
        }

        override fun run() {

            val period = 1000.0 / 30 // milliseconds per tick
            var last = System.currentTimeMillis()
            var current = last
            var difference = 0.0

            while (!isInterrupted) {

                last = current
                current = System.currentTimeMillis()
                difference += (current - last).toDouble()

                while (difference > period) {
                    difference -= period
                    controller.session.stepCursor = controller.session.correctedStepCursor + controller.movementDirection(controller.session.lastX)
                    controller.session.updateSwapWith()

                }

            }

            controller.session.executeSwap()

        }

    }

}