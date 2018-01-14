package core

import core.Note.Companion.noteLetterShort
import core.Note.Companion.noteString
import java.io.Serializable

data class Chord(val recordingStart: Int, val noteStart: Int, val notes: MutableList<Note>, val pattern: ChordPattern) : Serializable {

    val rootPitch
        get() = notes[0].pitch

    fun asString(): String = "${rootPitch.noteLetterShort}${pattern.suffix}"

    internal val isValid: Boolean
        get() {
            val root = notes.find { possibleRoot ->
                // none of the notes from pattern are missed out
                pattern.notes.none {
                    (it + possibleRoot.pitch) !in notes.map { it.pitch }
                }
            }
            return if (root == null) {
                false
            } else {
                notes.remove(root)
                notes.add(0, root)
                true
            }
        }

    internal val isPossible: Boolean
        get() {
            val root = notes.find { possibleRoot ->
                notes.all {
                    it.pitch - possibleRoot.pitch in pattern.notes
                }
            }
            return if (root == null) {
                false
            } else {
                notes.remove(root)
                notes.add(0, root)
                true
            }
        }

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

    class ChordPattern private constructor(val title: String, val suffix: String, val notes: List<Int>) {
        constructor(title: String, suffix: String, vararg notes: Int) : this(title, suffix, notes.asList())
    }

}

class PatternMatcher : Serializable {

    internal val chords
        get() = chordStates.map { it.chord!! }

    private val chordStates: MutableList<PatternMatchingState> = mutableListOf()
    private val states: MutableList<PatternMatchingState> = mutableListOf()
    private val liveStates: MutableList<PatternMatchingState> = mutableListOf()

    fun feed(note: Note) {

        val newState = PatternMatchingState(states.size)
        states.add(newState)
        liveStates.add(newState)

        val newChord = mutableListOf<PatternMatchingState>()
        liveStates.forEach { it.add(note) }
        liveStates.removeIf {
            if (it.chord != null) {
                newChord.add(it)
            }
            return@removeIf it.isDead
        }

        newChord.forEach {
            if (chordStates.isEmpty()) {
                chordStates.add(it)
            } else {
                if (!chordStates.last().isDead && chordStates.last().notes.all { last ->
                    it.notes.contains(last)
                }) { // if the last chord added contains all the notes of this chord
                    chordStates[chordStates.lastIndex] = it
                } else {
                    chordStates.add(it)
                }
            }
        }

    }

    fun feed(notes: MutableList<Note>) {
        notes.forEach { feed(it) }
    }

    fun clear() {

        chordStates.clear()
        states.clear()
        liveStates.clear()

    }

}

internal class PatternMatchingState(private val index: Int) : Serializable {

    val notes = mutableListOf<Note>()
    val start
        get() = notes.minBy { it.start }!!

    var isDead = false
    var possibleChords = listOf<Chord>()

    var chord: Chord? = null

    fun add(note: Note) {
//            if (index == 0) {
//                println(notes.size)
//            }
        notes.add(note)
        if (notes.isEmpty()) {
            possibleChords = Chord.chordPatters.map { pattern ->
                Chord(note.start, index, mutableListOf(note), pattern)
            }
        } else {
//                if (index == 0)
//                    print(possibleChords.map { it.pattern.name }.distinct())
            possibleChords.forEach { it.notes.add(note) }
            possibleChords = possibleChords.filter { it.isPossible }

            //                if (index == 0)
//                    println(" ${possibleChords.map { it.pattern.name }.distinct()}")
            if (possibleChords.isEmpty()) {
                isDead = true
            } else {
//                    println("$notes is already ${
//                        possibleChords.filter { chord ->
//                            chord.pattern.notes.all { note ->
//                                notes.any {
//                                    it.pitch == note + chord.rootPitch
//                                }
//                            }
//                        }
//                    }")
                val chordFound = possibleChords.filter { it.isValid }.maxBy { it.pattern.notes.size }
                if (chordFound != null) {
                    chord = chordFound
                }

            }
        }

//            if (index == 0) {
//                println("notes=${notes.map { it.pitch.noteStringShort }}, dead=$isDead, chord=$chord, pos=${possibleChords.map { it.asString() }}")
//            }

    }

    override fun toString(): String {
        return "ChordFSMState(isDead=$isDead, ${possibleChords.map { it.rootPitch.noteString + it.pattern.title }})"
    }

    companion object {

        internal infix fun Int.floorMod(other: Int) = ((this % other) + other) % other

    }

}