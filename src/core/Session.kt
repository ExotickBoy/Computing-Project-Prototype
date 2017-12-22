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

    private val updateCallbacks: MutableList<() -> Unit> = mutableListOf()
    var width = 500

    val analyser: Analyser = Analyser(this)
    var cursor: Int? = null
        set(value) {
            synchronized(recording) {
                val toBecome = if (value == null || value >= recording.length) null else max(value, 0)
                if (toBecome != field) {
                    field = toBecome
                    updateLocations()
                    runCallbacks()
                }
            }
        }

    val correctedCursor: Int
        get() = cursor ?: (recording.length - 1)

    var onScreenCursor: Int = 0
    var from: Int = 0
    var to: Int = 0
    val visibleRange
        get() = from..to

    private fun updateLocations() {
        onScreenCursor = min(max(width - (recording.length - correctedCursor), width / 2), correctedCursor)
        from = max(correctedCursor - onScreenCursor, 0)
        to = min(correctedCursor + (width - onScreenCursor), recording.length)
    }

    var swap: Int? = null

    fun start() {
        analyser.start()
        cursor = null
    }

    fun pause() {
        analyser.pause()
    }

    fun resume() {
        analyser.resume()
        cursor = null
    }

    /**
     * Adds a new TimeStep through the session to the recording
     */
    fun addTimeStep(timeStep: TimeStep) {

        recording.addTimeStep(timeStep)
        updateLocations()
        runCallbacks()

    }

    /**
     * Cuts the recording at the cursor location and invokes update listeners
     * @param cursor The time at which the cut should happen
     */
    fun makeCut(cursor: Int) {
        recording.cut(cursor)
        runCallbacks()
    }

    /**
     * Adds an onUpdate listener which is called every time one of the properties of session changes
     * @param callback The lambda that will be invoked when an update happens
     */
    fun addOnUpdateListener(callback: () -> Unit) {

        updateCallbacks.add(callback)

    }

    /**
     * Invokes all the onUpdateListeners
     */
    private fun runCallbacks() {
        updateCallbacks.forEach { it.invoke() }
    }

}