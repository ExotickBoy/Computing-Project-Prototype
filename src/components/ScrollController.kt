package components

import core.Session
import java.awt.Component
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import kotlin.math.*

/**
 * This class is the mouse listener that is reused int he various output panes.
 * It controls scrolling of the cursor and swapping of sections
 * @property isNote If the scrolling should wrap to the closest note
 * @property session The session that this controller is responsible for
 */
internal class ScrollController(private val isNote: Boolean, private val component: Component, private val session: Session) : MouseMotionListener, MouseListener {

    private var longPressTimer: LongPressThread? = null
    private var draggingThread: DraggingThread? = null

    override fun mouseMoved(e: MouseEvent) {
        longPressTimer?.interrupt()
    }

    override fun mouseDragged(e: MouseEvent) {
        longPressTimer?.interrupt()

        val dx = e.x - session.lastX

        synchronized(session.recording) {

            if (session.state == Session.SessionState.EDIT_SAFE && dx != 0 && draggingThread?.isAlive != true) {
                // im using != true, because it cold be null

                if (!isNote) {

                    session.stepCursor = max(min(session.correctedStepCursor - dx, session.recording.timeStepLength), 0)

                } else {
                    session.clusterCursor = session.correctedClusterCursor - session.clusterWidth * dx / session.width
                }

            }
            session.lastX = e.x
            session.lastY = e.y / component.height.toDouble()
        }

        e.consume()

    }

    override fun mousePressed(e: MouseEvent) {

        session.lastX = e.x
        session.lastY = e.y / component.height.toDouble()

        longPressTimer?.interrupt()
        longPressTimer = LongPressThread(this)
    }

    override fun mouseReleased(e: MouseEvent) {
        longPressTimer?.interrupt()
        draggingThread?.interrupt()
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}

    override fun mouseClicked(e: MouseEvent) {

        if (!isNote) {
            session.stepCursor = session.stepFrom + e.x
            e.consume()
        }
    }

    private fun mouseLongPressed() {

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

    private fun movementDirection(x: Int): Int {

        return ((1.025.pow(max(abs(x - session.width / 2.0) - session.width / 5.0, 0.0)) - 1) * sign(x - session.width / 2.0)).toInt()

    }

    companion object {

        const val holdTime: Int = (.3 * 1000).toInt()

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

    private class LongPressThread(private val controller: ScrollController) : Thread() {

        private val endAt = System.currentTimeMillis() + holdTime

        init {

            name = "Timer Thread"
            start()

        }

        override fun run() {

            while (System.currentTimeMillis() < endAt) {
                onSpinWait()
            }
            if (!isInterrupted)
                controller.mouseLongPressed()

        }

    }

}