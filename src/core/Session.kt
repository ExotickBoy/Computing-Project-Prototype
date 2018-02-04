package core

import javax.sound.sampled.LineUnavailableException
import javax.swing.JOptionPane
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

    private val onUpdated: MutableList<() -> Unit> = mutableListOf()
    private val onEdit: MutableList<() -> Unit> = mutableListOf()

    private var stepCursorField: Int? = 0
    private var clusterCursorField: Double? = 0.0

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
                    onUpdated()
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
                    onUpdated()
                }
            }

        }
        get() = clusterCursorField

    val correctedStepCursor: Int
        get() {
            synchronized(recording) {
                return stepCursor ?: max(recording.timeStepLength - 1, 0)
            }
        }
    val correctedClusterCursor: Double
        get() {
            synchronized(recording) {
                return clusterCursor ?: (recording.clusterLength.toDouble())
            }
        }
    var onScreenStepCursor: Int = 0
        private set
    var onScreenClusterCursor: Double = 0.0
        private set
    var stepFrom: Int = 0
        private set
    var clusterFrom: Double = 0.0
        private set

    private var stepTo: Int = 0
    private var clusterTo: Double = 0.0

    val visibleStepRange
        get() = stepFrom..stepTo
    val visibleClusterRange
        get() = clusterFrom..clusterTo

    var width: Int = 0
        set(value) {
            field = value
            updateLocations()
            onUpdated()
        }
    var clusterWidth: Double = 0.0
        set(value) {
            field = value
            updateLocations()
            onUpdated()
        }

    var lastX: Int = 0
        set(value) {
            field = value
            onUpdated()
        }
    var lastY: Double = 0.0
        set(value) {
            field = value
            onUpdated()
        }

    var swap: Int? = null
        set(value) {
            field = value
            onUpdated()
        }
    var swapWith: Int = 0
    var swapWithSection: Boolean? = false

    val state: SessionState
        get() = when {
            !playbackController.isPaused -> SessionState.PLAYING_BACK
            swap != null -> SessionState.SWAPPING
            recording.isProcessed -> SessionState.EDIT_SAFE
            recording.isPreProcessed -> SessionState.PROCESSING
            recording.isGathered -> SessionState.PRE_PROCESSING
            else -> SessionState.GATHERING
        }

    var isEdited = false
        private set (value) {
            field = value
        }

    private val microphoneController = MicrophoneController(this)
    private val soundProcessingController = SoundProcessingController(this)
    private val playbackController = PlaybackController(this)

    init {
        if (!recording.isEmpty && !recording.isProcessed) {
            recording.sections.takeLastWhile { !it.isProcessed }.forEach(soundProcessingController::fastProcess)
        }

        soundProcessingController.begin()
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

    fun toggleMute(): Boolean {

        onUpdated()
        return playbackController.toggleMute()

    }

    fun dispose() {

        microphoneController.end()
        soundProcessingController.end()
        playbackController.end()

    }

    /**
     * Starts recording sound from the microphone
     * @return If this was performed successfully
     */
    fun record(): Boolean {
        if (state == SessionState.EDIT_SAFE) {
            synchronized(recording) {
                if (!microphoneController.isOpen)
                    try {
                        microphoneController.begin()
                    } catch (e: Exception) {

                        JOptionPane.showMessageDialog(null,
                                "Failed to open microphone\n" + when (e) {
                                    is LineUnavailableException -> "Couldn't find a valid microphone"
                                    is IllegalArgumentException -> "Couldn't find a valid microphone"
                                    else -> "Unknown error occurred"
                                }, "Error", JOptionPane.ERROR_MESSAGE)
                        return false

                    }

                stepCursor = null
                microphoneController.isPaused = false

                onUpdated()

                return true

            }

        } else return false

    }

    /**
     * Pauses the recording
     * @return If this was performed successfully
     */
    fun pauseRecording(): Boolean {
        return if (!microphoneController.isPaused) {
            microphoneController.isPaused = true
            true
        } else false
    }

    /**
     * Starts the playback of the sound
     * @return If this was performed successfully
     */
    fun playback(): Boolean {
        return if (state == SessionState.EDIT_SAFE && stepCursor != null) {
            if (!playbackController.isOpen)
                if (!playbackController.begin()) return false
            // if it couldn't begin the playback controller
            playbackController.isPaused = false
            onUpdated()
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
            true
        } else {
            false
        }
    }

    fun addOnUpdate(callback: () -> Unit) {
        onUpdated.add(callback)
    }

    fun addOnEdited(callback: () -> Unit) {
        onEdit.add(callback)
    }

    internal fun onUpdated() {

        onUpdated.forEach { it.invoke() }
    }

    internal fun onEdited() {

        updateLocations()
        isEdited = true
        onEdit.forEach { it.invoke() }
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
                    // this is when the cursor is off the dispose of the recording
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
                onEdited()
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

    enum class SessionState {
        GATHERING,
        PRE_PROCESSING,
        PROCESSING,
        EDIT_SAFE,
        SWAPPING,
        PLAYING_BACK
    }

}
