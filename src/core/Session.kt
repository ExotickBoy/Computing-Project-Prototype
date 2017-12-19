package core

/**
 * This class is for storing a session of editing the recording
 * @see Recording
 * @see Analyser
 * @author Kacper Lubisz
 * @property recording  The recording that concerns this session
 * @property analyser The analyser for this current session
 * @property updateCallbacks A list of lambdas used for adding onUpdate listeners to anything that needs it
 * @property cursor The position of the cursor for editing (-1 means the end)
 * @property swap A nullable index of the current section being held to be swapped
 */
class Session(val recording: Recording) {

    val analyser: Analyser = Analyser(this)
    val updateCallbacks: MutableList<() -> Unit> = mutableListOf()
    var cursor = -1
        set(value) {
            if (value != field) {
                field = value
                runCallbacks()
            }
        }
    var swap: Int? = null

    /**
     * Adds a new TimeStep through the session to the recording
     */
    fun addTimeStep(timeStep: TimeStep) {

        recording.addTimeStep(timeStep)
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

    companion object {

        const val swapModeZoom = 5.0

    }

}