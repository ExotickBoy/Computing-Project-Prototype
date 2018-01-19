package core

import kotlin.math.max
import kotlin.math.min

/**
 * This object stores all the properties relevant to the current session of editing a recording
 *
 * @author Kacper Lubisz
 *
 * @see Recording
 * @see Session
 *
 * @property recording The recording that this session is to edit
 * @property stepCursor The cursor of the recording measured in TimeSteps
 * @property clusterCursor The cursor of the recording measured in NoteClusters
 */
class Session(val recording: Recording) {

    private val onStepChange: MutableList<() -> Unit> = mutableListOf()
    private val onCursorChange: MutableList<() -> Unit> = mutableListOf()
    private val onStateChange: MutableList<() -> Unit> = mutableListOf()
    private val onSwapChange: MutableList<() -> Unit> = mutableListOf()

    private var stepCursorField: Int? = null
    private var clusterCursorField: Double? = null

    var stepCursor: Int?
        set(value) {
            synchronized(recording) {
                val toBecome = if (value == null || value >= recording.timeStepLength) null else max(value, 0)
                if (toBecome != stepCursorField) {
                    stepCursorField = toBecome

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
        get() = stepCursorField

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

                    println(clusters)

                    stepCursorField = (when {
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
    var lastY: Double = 0.0
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
    var swapWithSection: Boolean? = false

    val isEditSafe: Boolean
        get() = soundGatheringController.isPaused && playbackController.isPaused && !soundProcessingController.isProcessing

    val isRecording
        get() = !soundGatheringController.isPaused

    private val soundGatheringController = SoundGatheringController(this, true)
    private val soundProcessingController = SoundProcessingController(this)
    private val playbackController = PlaybackController(this) {
        onStateChange()
    }

    /**
     * Updates the onScreen locations of the cursors in different spaces and the range in that space that should be visible
     */
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

    /**
     * Starts recording sound from the microphone
     * @return If this was performed successfully
     */
    fun record(): Boolean {
        return if (isEditSafe) {
            try {
                synchronized(recording) {
                    if (!soundGatheringController.isOpen)
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

    /**
     * Pauses the recording
     * @return If this was performed successfully
     */
    fun pauseRecording(): Boolean {
        return if (!soundGatheringController.isPaused) {
            soundGatheringController.isPaused = true
            recording.endSection()
            onStateChange()
            true
        } else false
    }

    /**
     * Starts the playback of the sound
     * @return If this was performed successfully
     */
    fun playback(): Boolean {
        return if (isEditSafe && stepCursor != null) {
            playbackController.isPaused = false
            onStateChange()
            true
        } else {
            false
        }
    }

    /**
     * Stops the playback of the sound
     * @return If this was performed successfully
     */
    fun pausePlayback(): Boolean {
        return if (!playbackController.isPaused) {
            playbackController.isPaused = true
            onStateChange()
            true
        } else {
            false
        }
    }

    /**
     * Adds samples to the recording
     * @param samples A batch of samples that is to be added to the recording
     */
    fun addSamples(samples: FloatArray) {
        synchronized(recording) {
            recording.addSamples(samples)
        }
    }

    /**
     * Adds a TimeStep to the recording
     * @param step The TimeStep that is to be added
     */
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
            recording.cut(cursor)
        }
        onStepChange()
    }

    /**
     * Add a listener that will be called when TimeSteps are changed, i.e. added or moved via a swap
     */
    fun addOnStepChange(callback: () -> Unit) {
        onStepChange.add(callback)
    }

    /**
     * Add a listener that will be called when the cursor is moved
     * @param callback The callback that will be invoked
     */
    fun addOnCursorChange(callback: () -> Unit) {
        onCursorChange.add(callback)
    }

    /**
     * Add a listener that will be called when the state of the session is changed
     * (recording, playing back, safe to edit)
     * @param callback The callback that will be invoked
     */
    fun addOnStateChange(callback: () -> Unit) {
        onStateChange.add(callback)
    }

    /**
     * Add a listener that will be called when a swap property is changed
     *
     */
    fun addOnSwapChange(callback: () -> Unit) {
        onSwapChange.add(callback)
    }

    /**
     * Invokes all the onStepChange listeners
     */
    private fun onStepChange() {
        onStepChange.forEach { it.invoke() }
    }

    /**
     * Invokes all the onCursorChange listeners
     */
    private fun onCursorChange() {
        onCursorChange.forEach { it.invoke() }
    }

    /**
     * Invokes all the onStateChange listeners
     */
    private fun onStateChange() {
        onStateChange.forEach { it.invoke() }
    }

    /**
     * Invokes all the onSwapChange listeners
     */
    private fun onSwapChange() {
        onSwapChange.forEach { it.invoke() }
    }

    /**
     * This function updates what type of swap is going to happen based on the lastX and lastY of the mouse
     */
    fun updateSwapWith() {
        synchronized(recording) {

            if (lastY < DELETE_DISTANCE / 2) {
                swapWithSection = null // this is to signify a delete
            } else {

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
                    // this is when the cursor is off the end of the recording
                    // therefore to insert at the last edge

                    swapWith = recording.sections.size
                    swapWithSection = false

                }
            }
        }
    }

    /**
     * Performs the swap and resets all the swap parameters
     */
    fun executeSwap() {
        synchronized(recording) {
            val swap = swap

            if (swap != null) { // make sure that a swap exists
                when (swapWithSection) {
                    true -> recording.swapSections(swap, swapWith)
                    false -> recording.reInsertSection(swap, swapWith)
                    null -> {
                        recording.removeSection(swap)
                    }
                }
                this.swap = null
            }
            stepCursor = correctedStepCursor
        }

    }

    /**
     * This is just a data class used locally for finding the absolute starts of all notes
     */
    private data class PlayedCluster(val recordingStart: Int, val index: Int, val section: Section) {
        override fun toString(): String {
            return "PlayedCluster(recordingStart=$recordingStart, index=$index)"
        }
    }

    companion object {
        const val DELETE_DISTANCE = .3
    }

}