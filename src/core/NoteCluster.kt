package core

import core.Note.Companion.noteLetterShort
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable


data class NoteCluster(private val root: Int, val relTimeStepStart: Int, val placements: List<Int>, val pattern: ChordPattern?) : Serializable {

    @Throws(IOException::class)
    private fun writeObject(output: ObjectOutputStream) {
        output.defaultWriteObject()
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
    }

    val heading: String
        get() = root.noteLetterShort + " " + (pattern?.suffix ?: "")

}