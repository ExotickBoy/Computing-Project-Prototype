package core

data class Chord(val recordingStart: Int, val rootPitch: Int, val pattern: ChordPattern) {

    companion object {

        val chordPatters = listOf(
                ChordPattern("Major", "", 0, 4, 7),
                ChordPattern("Minor", "m", 0, 3, 7),
                ChordPattern("Diminished", "dim", 0, 3, 6),
                ChordPattern("Augmented", "aug", 0, 4, 8),
                ChordPattern("+7", "7", 0, 7),
                ChordPattern("+5", "5", 0, 5)
        )

    }

    data class ChordPattern(val name: String, val suffix: String, val notes: List<Int>) {
        constructor(name: String, suffix: String, vararg notes: Int) : this(name, suffix, notes.asList())
    }
}

class ChordFSMM {

    val chords: MutableList<Chord> = mutableListOf()

    val states: MutableList<ChordFSMState> = mutableListOf()
    val liveStates: MutableList<ChordFSMState> = mutableListOf()

    fun feed(note: Note) {

        val newState = ChordFSMState()
        states.add(newState)
        liveStates.add(newState)

        liveStates.forEach { it.add(note) }
        liveStates.removeIf { it.isDead }

    }

    class ChordFSMState {

        val notes = mutableListOf<Note>()
        val start
            get() = notes.minBy { it.start }!!
        var isDead = false

        fun add(note: Note) {

            notes.add(note)

            notes.flatMap { root ->
                Chord.chordPatters.filter { pattern ->
                    notes.map { (it.pitch - root.pitch + 12) % 12 }.all { pattern.notes.contains(it) }
                }.map {
                    Chord(start.start, root.pitch, it)
                }
            }

        }

    }

}

