package core

import kotlin.math.max
import kotlin.math.min

/**
 * This class is for storing a session of editing the recording
 * @see Recording
 * @see Analyser
 * @author Kacper Lubisz
 * @property recording  The recording that concerns this session
 * @property analyser The analyser for this current session
 * @property updateCallbacks A list of lambdas used for adding onUpdate listeners to anything that needs it
 * @property cursor The position of the cursor for editing (null means the end)
 * @property swap A nullable index of the current section being held to be swapped
 */
class Session(val recording: Recording) {

    private val onStepChange: MutableList<() -> Unit> = mutableListOf()
    private val onCursorChange: MutableList<() -> Unit> = mutableListOf()
    private val onStateChange: MutableList<() -> Unit> = mutableListOf()
    private val onSwapChange: MutableList<() -> Unit> = mutableListOf()
    private val onClusterChange: MutableList<() -> Unit> = mutableListOf()

    private var cursorField: Int? = null
    private var clusterCursorField: Double? = null

    var stepCursor: Int?
        set(value) {
            synchronized(recording) {
                val toBecome = if (value == null || value >= recording.timeStepLength) null else max(value, 0)
                if (toBecome != cursorField) {
                    cursorField = toBecome

                    val clusters = recording.sections.flatMap {
                        it.clusters.mapIndexed { index, cluster ->
                            return@mapIndexed PlayedCluster(cluster.relTimeStepStart + it.timeStepStart, it.clusterStart + index, it)
                        }
                    }
                    val after = clusters.mapIndexed { index, it -> index to it }.find { it.second.recordingStart > correctedStepCursor }?.first
                    clusterCursorField = when {
                        toBecome == null -> null
                        clusters.isEmpty() -> 0.0
                        after == null -> clusters.size.toDouble() + 0.5 * (correctedStepCursor - clusters[clusters.size - 1].recordingStart) / (recording.timeStepLength - clusters[clusters.size - 1].recordingStart) - 0.5
                        after == 0 -> 0.5 * correctedStepCursor / clusters[0].recordingStart
                        else -> {
                            val next = clusters[after]
                            val previous = clusters[after - 1]
                            val between = when {
                                next.section == previous.section -> // no cut
                                    (correctedStepCursor - previous.recordingStart) / (next.recordingStart - previous.recordingStart).toDouble()
                                correctedStepCursor <= next.section.timeStepStart -> // left side of cut
                                    0.5 * (correctedStepCursor - previous.recordingStart) / (next.section.timeStepStart - previous.recordingStart).toDouble()
                                else -> // right side of cut
                                    0.5 + 0.5 * (correctedStepCursor - next.section.timeStepStart) / (next.recordingStart - next.section.timeStepStart).toDouble()
                            }
                            after.toDouble() + between - 0.5
                        }
                    }

                    updateLocations()
                    onCursorChange()
                }

            }
        }
        get() = cursorField

    var clusterCursor: Double?
        set(value) {
            synchronized(recording) {

                val toBecome = if (value == null || value >= recording.clusterLength) null else max(value, 0.0)
                if (toBecome != clusterCursorField) {
                    clusterCursorField = toBecome

                    val clusters = recording.sections.flatMap {
                        it.clusters.mapIndexed { index, cluster ->
                            return@mapIndexed PlayedCluster(cluster.relTimeStepStart + it.timeStepStart, it.clusterStart + index, it)
                        }
                    }
                    cursorField = (when {
                        toBecome == null -> null
                        toBecome > clusters.size + 0.5 -> null
                        clusters.isEmpty() -> recording.timeStepLength * toBecome
                        toBecome < 0.5 -> toBecome * 2 * clusters[0].recordingStart
                        toBecome > clusters.size - 0.5 ->
                            clusters[clusters.size - 1].recordingStart + (0.5 + toBecome - clusters.size) * 2 * +(recording.timeStepLength - clusters[clusters.size - 1].recordingStart)
                        else -> {
                            val before = (toBecome - 0.5).toInt()
                            val inter = toBecome - 0.5 - before

                            val next = clusters[(toBecome + 0.5).toInt()]
                            val previous = clusters[(toBecome - 0.5).toInt()]
                            when {
                                next.section == previous.section ->
                                    previous.recordingStart * (1 - inter) + next.recordingStart * inter
                                inter < 0.5 ->
                                    previous.recordingStart * (1 - inter * 2) + next.section.timeStepStart * inter * 2
                                else ->
                                    next.section.timeStepStart * (2 - inter * 2) + next.recordingStart * (inter * 2 - 1)
                            }

                        }
                    })?.toInt()

                    updateLocations()
                    onCursorChange()
                }
            }

        }
        get() = clusterCursorField

    val correctedStepCursor: Int
        get() {
            synchronized(recording) {
                return stepCursor ?: (recording.timeStepLength - 1)
            }
        }
    val correctedClusterCursor: Double
        get() {
            synchronized(recording) {
                return clusterCursor ?: (recording.clusterLength.toDouble())
            }
        }
    var onScreenStepCursor: Int = 0
    var onScreenClusterCursor: Double = 0.0
    var stepFrom: Int = 0
    var stepTo: Int = 0
    var clusterFrom: Double = 0.0
    var clusterTo: Double = 0.0

    val visibleStepRange
        get() = stepFrom..stepTo
    val visibleClusterRange
        get() = clusterFrom..clusterTo

    var width: Int = 0
        set(value) {
            field = value
            onCursorChange()
        }
    var clusterWidth: Double = 0.0

    var lastX: Int = 0
        set(value) {
            field = value
            onSwapChange()
        }


    var swap: Int? = null
        set(value) {
            field = value
            onSwapChange()
        }

    var swapWith: Int = 0
    var swapWithSection: Boolean = false

    val isEditSafe: Boolean
        get() = soundGatheringController.isPaused && playbackController.isPaused

    val isRecording
        get() = !soundGatheringController.isPaused

    private val soundGatheringController = SoundGatheringController(this)
    private val soundProcessingController = SoundProcessingController(this)
    private val playbackController = PlaybackController(this) {
        onStateChange()
    }

    private fun updateLocations() {
        synchronized(recording) {
            onScreenStepCursor = min(max(width - (recording.timeStepLength - correctedStepCursor), width / 2), correctedStepCursor)
            stepFrom = max(correctedStepCursor - onScreenStepCursor, 0)
            stepTo = min(correctedStepCursor + (width - onScreenStepCursor), recording.timeStepLength)

            onScreenClusterCursor = min(max(clusterWidth - (recording.clusterLength - correctedClusterCursor), clusterWidth / 2), correctedClusterCursor)
            clusterFrom = max(correctedClusterCursor - onScreenClusterCursor, 0.0)
            clusterTo = min(correctedClusterCursor + (clusterWidth - onScreenClusterCursor), recording.clusterLength.toDouble())
        }
    }

    fun record(): Boolean {
        return if (isEditSafe) {
            try {
                synchronized(recording) {

                    if (!soundGatheringController.isAlive)
                        soundGatheringController.start()

                    stepCursor = null
                    soundGatheringController.isPaused = false

                    recording.startSection()
                    onStateChange()

                    true

                }
            } catch (e: Exception) {

                println("Couldn't open microphone line")
                false

            }
        } else {
            false
        }
    }

    fun pauseRecording(): Boolean {
        return if (!soundGatheringController.isPaused) {
            soundGatheringController.isPaused = true
            onStateChange()
            true
        } else false
    }

    fun playback(): Boolean {
        return if (isEditSafe && stepCursor != null) {
            playbackController.isPaused = false
            onStateChange()
            true
        } else {
            false
        }
    }

    fun pausePlayback(): Boolean {
        return if (!playbackController.isPaused) {
            playbackController.isPaused = true
            onStateChange()
            true
        } else {
            false
        }
    }

    fun addSamples(samples: FloatArray) {
        synchronized(recording) {
            recording.addSamples(samples)
        }
    }

    fun addTimeStep(step: TimeStep) {
        synchronized(recording) {
            recording.addTimeStep(step)
            updateLocations()
        }
        onStepChange()
    }

    /**
     * Cuts the recording at the cursor location and invokes update listeners
     * @param cursor The time at which the cut should happen
     */
    fun makeCut(cursor: Int) {
        synchronized(recording) {
            println("CUTTING")
            recording.cut(cursor)
            println("CUT")
            println(recording.sections)
        }
        onStepChange()
    }

    fun addOnClusterChange(callback: () -> Unit) {
        onClusterChange.add(callback)
    }

    fun addOnStepChange(callback: () -> Unit) {
        onStepChange.add(callback)
    }

    fun addOnCursorChange(callback: () -> Unit) {
        onCursorChange.add(callback)
    }

    fun addOnStateChange(callback: () -> Unit) {
        onStateChange.add(callback)
    }

    fun addOnSwapChange(callback: () -> Unit) {
        onSwapChange.add(callback)
    }

    private fun onClusterChange() {
        onClusterChange.forEach { it.invoke() }
    }

    private fun onStepChange() {
        onStepChange.forEach { it.invoke() }
    }

    private fun onCursorChange() {
        onCursorChange.forEach { it.invoke() }
    }

    private fun onStateChange() {
        onStateChange.forEach { it.invoke() }
    }

    private fun onSwapChange() {
        onSwapChange.forEach { it.invoke() }
    }

    fun updateSwapWith() {
        synchronized(recording) {

            val location = lastX + stepFrom
            val sectionIndex = recording.sectionAt(location)

            if (sectionIndex != null) {

                val section = recording.sections[sectionIndex]
                val distanceFromEdge = min(10, (section.timeSteps.size * .2).toInt())

                when {
                    location - section.timeStepStart < distanceFromEdge -> { // left edge

                        swapWith = sectionIndex
                        swapWithSection = false

                    }
                    section.timeStepEnd - location < distanceFromEdge -> { // right edge

                        swapWith = sectionIndex + 1
                        swapWithSection = false

                    }
                    else -> { // section

                        swapWith = sectionIndex
                        swapWithSection = true

                    }
                }

            } else {

                swapWith = recording.sections.size
                swapWithSection = false

            }
        }
    }

    fun executeSwap() {
        synchronized(recording) {
            val swap = swap

            if (swap != null) {
                if (swapWithSection)
                    recording.swapSections(swap, swapWith)
                else
                    recording.reInsertSection(swap, swapWith)

                this.swap = null
            }
        }

    }

    private data class PlayedCluster(val recordingStart: Int, val index: Int, val section: Section)

}