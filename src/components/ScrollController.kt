package components

import core.Session
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import kotlin.math.max
import kotlin.math.min

/**
 * This class is the mouse listener that is reused int he various output panes.
 * It controls scrolling of the cursor and swapping of sections
 * @property isNote If the scrolling should wrap to the closest note
 * @property session The session that this controller is responsible for
 */
class ScrollController(val isNote: Boolean, val session: Session) : MouseMotionListener, MouseListener {

    private var lastX = 0
    private var longPressTimer: LongPressThread? = null

    override fun mouseMoved(e: MouseEvent) {
        longPressTimer?.end()
    }

    override fun mouseDragged(e: MouseEvent) {
        longPressTimer?.end()

        if (!session.analyser.isRunning) {

            val dx = e.x - lastX
            lastX = e.x

            if (!isNote) {

                val before = (if (session.cursor == -1)
                    session.recording.length - 1
                else
                    session.cursor)


                val after = max(min(before - dx, session.recording.length), 0)


                if (after == session.recording.length) {
                    session.cursor = -1
                } else {
                    session.cursor = after
                }

            }

        }

    }

    override fun mousePressed(e: MouseEvent) {

        lastX = e.x

        longPressTimer?.end()
        longPressTimer = LongPressThread(this)

    }

    override fun mouseReleased(e: MouseEvent) {
        longPressTimer?.end()
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}

    override fun mouseClicked(e: MouseEvent) {}

    fun mouseLongPressed() {
        println("longPress")
        session.swap = session.recording.sectionAt(lastX + session.cursor
//                - session.onScreenCursor
        )
        println(session.swap ?: "null")
    }

    companion object {

        const val holdTime: Int = (5 * 1000).toInt()

    }

    class LongPressThread(private val controller: ScrollController) : Thread() {

        private val startTime = System.currentTimeMillis()
        private val endAt = startTime + holdTime
        private var stopped = false

        init {

            name = "Timer Thread"
            start()

        }

        override fun run() {

            while (System.currentTimeMillis() < endAt && !stopped) {
            }
            if (!stopped) {
                controller.mouseLongPressed()
            }

        }

        internal fun end() {
            stopped = true
        }

    }

}