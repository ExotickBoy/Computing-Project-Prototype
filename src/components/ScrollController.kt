package components

import core.Session
import javafx.animation.PauseTransition
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.util.Duration
import kotlin.math.*

/**
 * This class packages the mouse listeners used for scrolling and can be added to different canvases
 *
 * @author Kacper Lubisz
 *
 * @property isNote If the scrolling should wrap to the closest note
 * @property willSwap If this owner will be able to start a swap
 * @property owner the canvas that will revieve the listeners
 * @property session The session that this controller is responsible for
 */
internal class ScrollController(private val isNote: Boolean, private val willSwap: Boolean, private val owner: Canvas, private val session: Session) {

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
                session.lastY = min(max(it.y / owner.height, 0.0), 1.0)
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

            if (!isNote && it.isPrimaryButtonDown && it.isStillSincePress && session.state == Session.SessionState.EDIT_SAFE) {
                session.stepCursor = (session.stepFrom + it.x).roundToInt()
                it.consume()
            }

        }

        val holdTimer = PauseTransition(Duration(holdTime))
        holdTimer.setOnFinished {
            if (session.state == Session.SessionState.EDIT_SAFE && !isNote && willSwap) {
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

    /**
     * This function us used to find how to move move the cursor based on where the mouse is.
     *
     * When the user is holding a section to swap it they should be able to place the cursor on the sides of the
     * window and have the cursor move in the direction in which they have moved their cursor
     * @param x the x coordinate of the cursor
     * @return the rate of change of cursor
     */
    private fun movementDirection(x: Int): Int {

        return ((1.025.pow(max(abs(x - session.width / 2.0) - session.width / 5.0, 0.0)) - 1) * sign(x - session.width / 2.0)).toInt()

    }

    companion object {

        /* How long the mouse needs to be held for it to classify as a long press */
        const val holdTime: Double = .3 * 1000

    }

    /**
     * This thread is solely resposible for listening to where the mouse cursor is and then moving the in app
     * cursor when in select mode
     */
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

            // when the drag is over

            controller.session.executeSwap()

        }

    }

}