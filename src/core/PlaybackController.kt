package core

import core.SoundGatheringController.SAMPLE_BUFFER_SIZE
import core.SoundGatheringController.floatsToBytes
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.min
import kotlin.math.roundToInt


internal class PlaybackController(private val session: Session, private val onEnd: () -> Unit) : Thread("Playback Thread") {

    var isPaused = true
        set(value) {
            field = value
            if (field) {
                playHead = session.correctedStepCursor
            }
        }
    var playHead = 0

    init {
        start()
    }

    override fun run() {

        val period = 1000 / SoundProcessingController.FRAME_RATE
        var last = System.currentTimeMillis();
        var current = last;
        var accumulated = 0.0;

        while (!isInterrupted) {

            last = current;
            current = System.currentTimeMillis();
            accumulated += current - last;

            if (!isPaused) {
                while (accumulated > period) {
                    accumulated -= period;

                    session.stepCursor = session.correctedStepCursor + 1
                    if (session.stepCursor == null) {
                        isPaused = true
                        onEnd.invoke()
                    }

                }

            } else {
                sourceLine?.flush()
                while (isPaused || !isOpen) {
                    onSpinWait()
                }
                // on resume
                current = System.currentTimeMillis()
                last = current
                accumulated = 0.0

                val sectionIndex = session.recording.sectionAt(session.correctedStepCursor)
                if (sectionIndex == null) {
                    isPaused = true
                    onEnd.invoke()
                } else {
                    currentSectionIndex = sectionIndex
                    val section = session.recording.sections[currentSectionIndex]
                    sectionPlayHead = (section.samples.size * (session.correctedStepCursor - section.timeStepStart) /
                            section.timeSteps.size.toDouble()).roundToInt()
                    println(sectionPlayHead)
                }
            }

        }

        }

    }

    fun end() {
        interrupt()
    }

}