package core

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.DataLine
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min


internal class PlaybackController(private val session: Session, private val onEnd: () -> Unit) : Thread("Playback Thread") {

    var isPaused = true
        set(value) {
            field = value
            if (field) {
                playHead = session.correctedStepCursor
            }
        }

    var playHead = 0

    private var sourceLine: SourceDataLine? = null

    val isOpen: Boolean
        get() = sourceLine != null && sourceLine!!.isOpen

    init {
        start()
    }

    override fun run() {

        val period = 1000 / SoundProcessingController.FRAME_RATE
        var last = System.currentTimeMillis();
        var current = last;
        var accumulated = 0.0;

        var currentSectionIndex = 0
        var sectionPlayHead = 0

        while (!isInterrupted && isOpen) {

            last = current;
            current = System.currentTimeMillis();
            accumulated += current - last;

            if (!isPaused) {
                while (accumulated > period) {
                    accumulated -= period;

                    if (session.stepCursor == null) {
                        isPaused = true
                        onEnd.invoke()
                    }

                    val section = session.recording.sections[currentSectionIndex]
                    val to = sectionPlayHead + SoundGatheringController.SAMPLE_BUFFER_SIZE

                    val data = ByteArray(SoundGatheringController.SAMPLE_BUFFER_SIZE * 2)
                    for (s in sectionPlayHead until min(to, section.samples.size)) {

                        val sample = (section.samples[s] * 32768.0f).toShort()
                        val high = ((sample.toInt() shr 8).toByte() and 0xFF.toByte())
                        val low = (sample and 0xFF).toByte()

                        data[2 * s] = high
                        data[2 * s + 1] = low

                    }
                    val offset = min(to, section.samples.size)
                    for (s in 0 until max(sectionPlayHead - section.samples.size, 0)) {

//                        data[2*(s + offset)] = session.recording.sections[currentSectionIndex + 1].samples[s]

                    }

                    sourceLine!!.write(data, 0, data.size)
                    sourceLine!!.flush()

                    session.stepCursor = session.correctedStepCursor + 1

                }
            } else {
                while (isPaused) {
                    onSpinWait()
                }
                current = System.currentTimeMillis()
                accumulated = 0.0

                val sectionIndex = session.recording.sectionAt(session.correctedStepCursor)
                if (sectionIndex == null) {
                    isPaused = true
                    onEnd.invoke()
                } else {
                    currentSectionIndex = sectionIndex
                }
            }

        }

    }

    fun open() {

        val sourceInfo = DataLine.Info(SourceDataLine::class.java, SoundGatheringController.AUDIO_FORMAT)
        val line = AudioSystem.getLine(sourceInfo) as SourceDataLine
        line.open(SoundGatheringController.AUDIO_FORMAT)
        line.start()
        sourceLine = line

    }

}