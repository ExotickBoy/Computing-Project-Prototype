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
            field = value && isOpen
        }

    private var sourceLine: SourceDataLine? = null

    val isOpen: Boolean
        get() = sourceLine != null && sourceLine!!.isOpen

    init {
        start()
    }

    override fun run() {

        val period = 1000.0 * SoundGatheringController.SAMPLE_BUFFER_SIZE / SoundProcessingController.SAMPLE_RATE
        var last = System.currentTimeMillis()
        var current = last
        var accumulated = 0.0

        val data = ByteArray(SoundGatheringController.SAMPLE_BUFFER_SIZE * 2)

        var currentSectionIndex = 0
        var sectionPlayHead = 0

        while (!isInterrupted) {

            last = current
            current = System.currentTimeMillis()
            accumulated += current - last

            if (!isPaused && isOpen) {
                while (accumulated > period) {
                    accumulated -= period

                    val section = session.recording.sections[currentSectionIndex]

                    val to = sectionPlayHead + SoundGatheringController.SAMPLE_BUFFER_SIZE

                    val currentFloats = min(to, section.samples.size) - sectionPlayHead
                    floatsToBytes(section.samples, data,
                            sectionPlayHead,
                            0,
                            currentFloats
                    )
                    if (currentSectionIndex != session.recording.sections.size - 1) {

                        floatsToBytes(session.recording.sections[currentSectionIndex + 1].samples, data,
                                0,
                                currentFloats,
                                SAMPLE_BUFFER_SIZE - currentFloats
                        )
                    }
                    sourceLine!!.write(data, 0, data.size)

                    sectionPlayHead = when {
                        to <= section.samples.size -> {
                            session.stepCursor = (section.timeStepStart + section.timeSteps.size.toDouble()
                                    * sectionPlayHead / section.samples.size).roundToInt()
                            to
                        }
                        currentSectionIndex == session.recording.sections.size - 1 -> {
                            isPaused = true
                            onEnd.invoke()

                            session.stepCursor = (section.timeStepStart + section.timeSteps.size.toDouble()
                                    * sectionPlayHead / section.samples.size).roundToInt()

                            section.samples.size
                        }
                        else -> {
                            currentSectionIndex += 1
                            val newSection = session.recording.sections[currentSectionIndex]
                            session.stepCursor = (newSection.timeStepStart + newSection.timeSteps.size.toDouble()
                                    * (to - section.samples.size) / newSection.samples.size).roundToInt()
                            to - section.samples.size
                        }
                    }
                }

            } else {
                sourceLine?.flush()
                while (isPaused || !isOpen) {
                    onSpinWait()
                }
                // on resume
                current = System.currentTimeMillis()
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

    fun open() {

        val sourceInfo = DataLine.Info(SourceDataLine::class.java, SoundGatheringController.AUDIO_FORMAT)
        val line = AudioSystem.getLine(sourceInfo) as SourceDataLine
        line.open(SoundGatheringController.AUDIO_FORMAT)
        line.start()
        sourceLine = line

    }

}