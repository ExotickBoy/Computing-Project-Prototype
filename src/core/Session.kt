package core

/**
 * This class is for storing the current interaction with a recording, including its analyser
 * TODO
 */
class Session(val recording: Recording) {

    val analyser: Analyser?

    var cursor = 0

    init {

        analyser = Analyser(this)

    }

    fun addTimeStep(timeStep: TimeStep) {

        recording.addTimeStep(timeStep)

    }

}