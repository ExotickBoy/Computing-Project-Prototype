package core

data class Chord(val recordingStart: Int, val noteStart: Int, val rootPitch: Int, val pattern: ChordPattern) {

    companion object {

        val chordPatters = listOf(
                ChordPattern("Major", "M", 0, 4, 7),
                ChordPattern("Minor", "m", 0, 3, 7),
                ChordPattern("Diminished", "d", 0, 3, 6),
                ChordPattern("Augmented", "a", 0, 4, 8),
                ChordPattern("+7", "7", 0, 7),
                ChordPattern("+5", "5", 0, 5)
        )

    }

    fun asString(): String = "${rootPitch.noteLetterShort}${pattern.suffix}"

    data class ChordPattern(val name: String, val suffix: String, val notes: List<Int>) {
        constructor(name: String, suffix: String, vararg notes: Int) : this(name, suffix, notes.asList())
    }
}

class ChordController {

    val chords: MutableList<Chord> = mutableListOf()

    val states: MutableList<ChordControllerState> = mutableListOf()
    private val liveStates: MutableList<ChordControllerState> = mutableListOf()

    fun feed(note: Note) {

        val newState = ChordControllerState(states.size)
        states.add(newState)
        liveStates.add(newState)

        liveStates.forEach { it.add(note) }
        val newChords = mutableListOf<ChordControllerState>()
        liveStates.removeIf {
            if (it.chord != null) {
                newChords.add(it)
            }

            return@removeIf it.isDead
        }

    }

    fun feed(notes: MutableList<Note>) {
        notes.forEach { feed(it) }
    }

    fun clear() {

        chords.clear()
        states.clear()
        liveStates.clear()

    }

    class ChordControllerState(private val index: Int) {

        val notes = mutableListOf<Note>()
        val start
            get() = notes.minBy { it.start }!!

        var isDead = false
        var possibleChords = listOf<Chord>()

        var chord: Chord? = null

        fun add(note: Note) {
            if (index == 0) {
                println(notes.size)
            }
            if (notes.isEmpty()) {
                possibleChords = Chord.chordPatters.flatMap { pattern ->
                    pattern.notes.map { root ->
                        Chord(note.start, index, note.pitch + root, pattern)
                    }
                }
                notes.add(note)
            } else {
                if (index == 0)
                    print(possibleChords.map { it.pattern.name }.distinct())
                possibleChords = possibleChords.filter { chord ->
                    chord.pattern.notes.any {
                        it == note.pitch - chord.rootPitch
                    }
                }
                if (index == 0)
                    println(" ${possibleChords.map { it.pattern.name }.distinct()}")
                if (possibleChords.isEmpty()) {
                    isDead = true
                } else {
                    possibleChords = possibleChords
                    notes.add(note)

                    chord = possibleChords.filter { chord ->
                        chord.pattern.notes.all { note ->
                            notes.any {
                                note == note - chord.rootPitch
                            }
                        }
                    }.maxBy { it.pattern.notes.size }

                }
            }

            if (index == 0) {
                println("notes=${notes.map { it.pitch.noteStringShort }}, dead=$isDead, chord=$chord, pos=${possibleChords.map { it.asString() }}")
            }

        }

        override fun toString(): String {
            return "ChordFSMState(isDead=$isDead, ${possibleChords.map { it.rootPitch.noteString + it.pattern.name }})"
        }

    }

}

infix fun Int.floorMod(other: Int) = ((this % other) + other) % other

