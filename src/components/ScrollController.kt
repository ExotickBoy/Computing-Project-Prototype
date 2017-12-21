package components

import core.Session
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * This class is the mouse listener that is reused int he various output panes.
 * It controls scrolling of the cursor and swapping of sections
 * @property isNote If the scrolling should wrap to the closest note
 * @property session The session that this controller is responsible for
 */
class ScrollController(val isNote: Boolean, val session: Session) : MouseMotionListener, MouseListener {

    private var lastX = 0
    private var longPressTimer: LongPressThread? = null
    private var draggingThread: DraggingThread? = null

    override fun mouseMoved(e: MouseEvent) {
        longPressTimer?.interrupt()
    }

    override fun mouseDragged(e: MouseEvent) {
        longPressTimer?.interrupt()

        if (!session.analyser.isRunning && draggingThread?.isAlive != true) {
            // im using == true, because it cold be null

            val dx = e.x - lastX
            lastX = e.x

            if (!isNote) {

                val before = (if (session.cursor == -1)
                    session.recording.length - 1
                else
                    session.cursor)


                val after = max(min(before - dx, session.recording.length), 0)

                session.cursor = after

            }

        }

    }

    override fun mousePressed(e: MouseEvent) {

        lastX = e.x

        longPressTimer?.interrupt()
        longPressTimer = LongPressThread(this)

    }

    override fun mouseReleased(e: MouseEvent) {
        longPressTimer?.interrupt()
        draggingThread?.interrupt()
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}

    override fun mouseClicked(e: MouseEvent) {}

    fun mouseLongPressed() {
        println("longPress")
        if (!session.analyser.isRunning) {
            session.swap = session.recording.sectionAt(lastX + session.cursor
                    - session.onScreenCursor
            )
            draggingThread = DraggingThread(this)
        }
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

            val mspt = 1000.0 / 30 // miliseconds per tick
            var last = System.currentTimeMillis()
            var current = last
            var difference = 0.0

            while (!isInterrupted) {

                println("dragging")

                last = current
                current = System.currentTimeMillis()
                difference += (current - last).toDouble()

                while (difference > mspt) {
                    difference -= mspt

                    controller.session.cursor = controller.session.cursor + controller.movementDirection(controller.lastX)
                }

            }

        }

    }

    fun movementDirection(x: Int): Int {

        return (0.2 * max(0.0, abs(x - session.width / 2) - 150.0) * sign(x - session.width / 2.0)).toInt()

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