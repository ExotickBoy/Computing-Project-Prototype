package core

// TODO
data class Section(private val recording: Recording, val timeStepStart: Int, val recordingStart: Int, val noteStart: Int, val length: Int?, val noteLength: Int?) {

    val correctedLength
        get() = length ?: recording.timeSteps.size - timeStepStart

    val correctedNoteLength
        get() = noteLength ?: recording.notes.size - noteStart

    val recordingRange
        get() = recordingStart until (recordingStart + correctedLength)

    val timeStepRange
        get() = timeStepStart until (timeStepStart + correctedLength)

    val noteRange
        get() = noteStart until (noteStart + correctedNoteLength)

    override fun toString(): String {
        return "Section(timeStepRange=$timeStepRange, recordingRange=$recordingRange, length=$length, correctedLength=$correctedLength, noteRange=$noteRange, noteLength=$noteLength, correctedNoteLength=$correctedNoteLength)"
    }

    companion object {

        const val minLength = 10

    }

}