package core

// TODO
data class Section(private val recording: Recording, val timeStepStart: Int, val recordingStart: Int, val noteStart: Int, val noteRecordingStart: Int, val length: Int?, val noteLength: Int?) {

    val correctedLength
        get() = length ?: recording.timeSteps.size - timeStepStart

    val correctedNoteLength
        get() = noteLength ?: recording.notes.size - noteStart

    val recordingRange
        get() = recordingStart until (recordingStart + correctedLength)

    val noteRange
        get() = noteStart until (noteStart + correctedNoteLength)

    override fun toString(): String {
        return "Section(recordingRange=$recordingRange, noteRange=$noteRange, length=$length, correctedLength=$correctedLength, noteLength=$noteLength, correctedNoteLength=$correctedNoteLength)"
    }

    companion object {

        const val minLength = 10

    }

}