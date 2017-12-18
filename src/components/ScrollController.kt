package components

import core.Session
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import kotlin.math.max
import kotlin.math.min

class ScrollController(val isNote: Boolean, val session: Session) : MouseMotionListener, MouseListener {

    private var lastX = 0

    override fun mouseMoved(e: MouseEvent) {}
    override fun mouseDragged(e: MouseEvent) {
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

    }

    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}

    override fun mouseClicked(e: MouseEvent) {

        if (e.button == MouseEvent.BUTTON2) { // Middle mouse
            session.swapMode = !session.swapMode
        }

    }

}