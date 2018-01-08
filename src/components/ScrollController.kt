package components

import core.Session
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
class ScrollController(private val isNote: Boolean, internal val session: Session) : MouseMotionListener, MouseListener {

    private var longPressTimer: LongPressThread? = null
    private var draggingThread: DraggingThread? = null

    override fun mouseMoved(e: MouseEvent) {
        longPressTimer?.interrupt()
    }

    override fun mouseDragged(e: MouseEvent) {
        longPressTimer?.interrupt()

        val dx = e.x - session.lastX

        if (dx != 0 && !session.analyser.isRunning && draggingThread?.isAlive != true) {
            // im using != true, because it cold be null

            if (!isNote) {
                session.cursor = max(min(session.correctedCursor - dx, session.recording.timeSteps.size), 0)
            } else {
                session.noteCursor = session.correctedNoteCursor - session.noteWidth * dx / session.width
            }

        }
        session.lastX = e.x

    }

    override fun mousePressed(e: MouseEvent) {

        session.lastX = e.x

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

        if (!isNote)
            session.cursor = session.from + e.x

    }

    private fun mouseLongPressed() {

        if (!session.analyser.isRunning && !isNote) {
            session.swap = session.recording.sectionAt(session.lastX + session.correctedCursor
                    - session.onScreenCursor
            )
            session.updateSwapWith()
            draggingThread = DraggingThread(this)
        }

    }

    private fun movementDirection(x: Int): Int {

        return ((1.025.pow(max(abs(x - session.width / 2.0) - session.width / 5.0, 0.0)) - 1) * sign(x - session.width / 2.0)).toInt()

    }

    companion object {

        const val holdTime: Int = (.5 * 1000).toInt()

    }

    class DraggingThread(private val controller: ScrollController) : Thread() {

        init {
            name = "Drag Thread"
            start()
        }

        override fun run() {

            val mspt = 1000.0 / 30 // milliseconds per tick
            var last = System.currentTimeMillis()
            var current = last
            var difference = 0.0

            while (!isInterrupted) {

                last = current
                current = System.currentTimeMillis()
                difference += (current - last).toDouble()

                while (difference > mspt) {
                    difference -= mspt
                    controller.session.cursor = controller.session.correctedCursor + controller.movementDirection(controller.session.lastX)
                    controller.session.updateSwapWith()

                }

            }

            controller.session.executeSwap()

        }

    }

    class LongPressThread(private val controller: ScrollController) : Thread() {

        private val endAt = System.currentTimeMillis() + holdTime

        init {

            name = "Timer Thread"
            start()

        }

        override fun run() {

            try {

                while (System.currentTimeMillis() < endAt) {
                    sleep(1)
                }

                controller.mouseLongPressed()

            } catch (e: InterruptedException) {
            }

        }

    }

}