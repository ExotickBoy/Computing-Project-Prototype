package core

import core.MicrophoneController.Companion.SAMPLE_BUFFER_SIZE
import core.SoundUtils.floatsToBytes
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.math.min
import kotlin.math.roundToInt


internal class PlaybackController(private val session: Session) : Thread("Playback Thread") {

    var isPaused = true
        set(value) {
            field = value && isOpen
        }

    var isMuted = false

    var currentSectionIndex = 0
    var sectionPlayHead = 0

    private var sourceLine: SourceDataLine? = null

    val isOpen: Boolean
        get() = sourceLine != null && sourceLine!!.isOpen

    fun begin(): Boolean {

        currentSectionIndex = session.recording.sectionAt(session.correctedStepCursor) ?: return false
        val section = session.recording.sections[currentSectionIndex]
        sectionPlayHead = (section.samples.size * ((session.correctedStepCursor - section.timeStepStart).toDouble() / section.timeSteps.size)).roundToInt()

        open()
        start()

        return true

    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        sourceLine?.flush()
        return isMuted
    }

    override fun run() {

        val period = 1000.0 * MicrophoneController.SAMPLE_BUFFER_SIZE / SoundProcessingController.SAMPLE_RATE
        var last = System.currentTimeMillis()
        var current = last
        var accumulated = 0.0

        val data = ByteArray(MicrophoneController.SAMPLE_BUFFER_SIZE * 2)

        while (!isInterrupted) {

            last = current
            current = System.currentTimeMillis()
            accumulated += current - last

            if (!isPaused && isOpen) {
                while (accumulated > period) {
                    accumulated -= period

                    val section = session.recording.sections[currentSectionIndex]

                    val to = sectionPlayHead + MicrophoneController.SAMPLE_BUFFER_SIZE

                    if (!isMuted) {
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
                    }

                    sectionPlayHead = when {
                        to <= section.samples.size -> {
                            session.stepCursor = (section.timeStepStart + section.timeSteps.size.toDouble()
                                    * sectionPlayHead / section.samples.size).roundToInt()
                            to
                        }
                        currentSectionIndex == session.recording.sections.size - 1 -> {
                            isPaused = true
                            session.onUpdated()

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
                while (isPaused && !isInterrupted) {
                    onSpinWait()
                }
                // on resume
                current = System.currentTimeMillis()
                accumulated = 0.0

                val sectionIndex = session.recording.sectionAt(session.correctedStepCursor)
                if (sectionIndex == null) {
                    isPaused = true
                    session.onUpdated()
                } else {
                    currentSectionIndex = sectionIndex
                    val section = session.recording.sections[currentSectionIndex]
                    sectionPlayHead = (section.samples.size * ((session.correctedStepCursor - section.timeStepStart).toDouble() / section.timeSteps.size)).roundToInt()

                }
            }

        }

    }

    private fun open() {

        val sourceInfo = DataLine.Info(SourceDataLine::class.java, MicrophoneController.AUDIO_FORMAT)
        val line = AudioSystem.getLine(sourceInfo) as SourceDataLine
        line.open(MicrophoneController.AUDIO_FORMAT)
        line.start()
        sourceLine = line
    }

    fun end() {
        interrupt()
        sourceLine?.close()
    }

}