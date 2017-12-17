package core

/**
 * This class is for storing the current interaction with a recording, including its analyser
 * TODO
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


    fun addTimeStep(timeStep: TimeStep) {

        recording.addTimeStep(timeStep)
        runCallbacks()

    }

    fun addCallback(callback: () -> Unit) {

        updateCallbacks.add(callback)

    }

    private fun runCallbacks() {
        updateCallbacks.forEach { it.invoke() }
    }

}