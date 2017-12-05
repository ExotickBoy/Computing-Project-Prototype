package core

/**
 * This class stores a note that should be played, it stores the pitch and start
 * @property pitch the pitch which the note represents
 * @property start the start at which the note is played
 * @author Kacper Lubisz
 */
data class Note(val pitch: Int, val start: Int, var duration: Int) {

    val end: Int
        get() = start + duration

}

const val SHARP = "♯"
const val FLAT = "♭"

private val noteLetters = listOf("C",
        "C$SHARP/D$FLAT",
        "D",
        "D$SHARP/E$FLAT",
        "E",
        "F",
        "F$SHARP/G$FLAT",
        "G",
        "G$SHARP/A$FLAT",
        "A",
        "A$SHARP/B$FLAT",
        "B")

/**
 * This extension function makes it easy to change a string into the pitch it represents
 */
val String.pitch: Int
    get() {

        if ("/" in this || "\\" in this)
            return this.replace("\\", "/").split("/".toRegex())
                    .map { it.pitch }
                    .find { it != -1 } ?: -1
        else if ("#" in this || "b" in this)
            return this.replace("#", SHARP).replace("b", FLAT).pitch
        else {

            val split = "[a-gA-G]($SHARP|$FLAT)?".toRegex().find(this) ?: return -1
            // split the note into the letter and the octave by finding the note part first

            val letter: String = this.substring(0, split.range.endInclusive + 1).toUpperCase()
            val octave: Int = this.substring(split.range.endInclusive + 1, this.length).toInt()

            val letterIndex = noteLetters.indexOf(noteLetters.find {
                it.split("/").any { it == letter }
            })

            return if (letterIndex == -1)
                -1
            else
                letterIndex + octave * 12


        }

    }

/**
 * This extension function makes it easy to change an int that represents a pitch to the string that describes it
 */
val Int.noteString: String
    get() = getLetter(this).replace("/", getOctave(this).toString() + "/") + getOctave(this)

/**
 * This extension function makes it easy to change a string into the note it represents
 */
fun String.note(time: Int, duration: Int): Note = Note(this.pitch, time, duration)

private fun getLetter(note: Int): String = noteLetters[note % 12]
private fun getOctave(note: Int): Int = note / 12
