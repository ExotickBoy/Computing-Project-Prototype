package core

import java.io.Serializable

/**
 * This class stores a note that should be played, it stores the pitch and start
 * @property pitch the pitch which the note represents
 * @property start the start at which the note is played
 * @author Kacper Lubisz
 */
class Note(val pitch: Int, val start: Int, var duration: Int) : Serializable {

    companion object {

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
        val String.pitch: Int?
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
                    val octave: Int? = this.substring(split.range.endInclusive + 1, this.length).toIntOrNull()

                    val letterIndex = noteLetters.indexOf(noteLetters.find {
                        it.split("/").any { it == letter }
                    })

                    return when {
                        octave == null -> null
                        letterIndex == -1 -> null
                        else -> letterIndex + octave * 12
                    }


                }

            }

        /**
         * This extension function makes it easy to change an int that represents a pitch to the string that describes it
         */
        val Int.noteString: String
            get() = this.noteLetter.replace("/", this.noteOctave.toString() + "/") + this.noteOctave

        val Int.noteStringShort: String
            get() = this.noteLetterShort.replace("/", this.noteOctave.toString() + "/") + this.noteOctave

        val Int.noteLetter: String
            get() = noteLetters[this % 12]

        val Int.noteLetterShort: String
            get() = noteLetters[this % 12].split("/")[0]

        val Int.noteOctave: Int
            get() = this / 12

    }

}