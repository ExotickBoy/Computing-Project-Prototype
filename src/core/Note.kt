package core

import java.io.Serializable

/**
 * This class stores a note that should be played, it stores the pitch and start
 *
 * @author Kacper Lubisz
 *
 * @property pitch the pitch which the note represents
 * @property start the start at which the note is played
 */
class Note(val pitch: Int, val start: Int, var duration: Int) : Serializable {

    companion object {

        private const val SHARP = "♯"
        private const val FLAT = "♭"
        private const val LAZY_SHARP = "#"
        private const val LAZY_FLAT = "b"
        private const val SEPARATOR = "/"

        private const val NOTE_LETTER_RANGE = "[a-gA-G]"

        private const val SPEMI_TONESP_PER_OCTAVE = 12

        private val noteLetters = listOf("C",
                "C$SHARP${SEPARATOR}D$FLAT",
                "D",
                "D$SHARP${SEPARATOR}E$FLAT",
                "E",
                "F",
                "F$SHARP${SEPARATOR}G$FLAT",
                "G",
                "G$SHARP${SEPARATOR}A$FLAT",
                "A",
                "A$SHARP${SEPARATOR}B$FLAT",
                "B")

        /**
         * This extension function makes it easy to change a string into the pitch it represents
         */
        val String.pitch: Int?
            get() {

                if ("/" in this || "\\" in this)
                    return this.replace("\\", SEPARATOR).split(SEPARATOR.toRegex())
                            .map { it.pitch }
                            .find { it != -1 } ?: -1
                else if (LAZY_SHARP in this || LAZY_FLAT in this)
                    return this.replace(LAZY_SHARP, SHARP).replace(LAZY_FLAT, FLAT).pitch
                else {
                    val split = "$NOTE_LETTER_RANGE($SHARP|$FLAT)?".toRegex().find(this) ?: return -1
                    // split the note into the letter and the octave by finding the note part first

                    val letter: String = this.substring(0, split.range.endInclusive + 1).toUpperCase()
                    val octave: Int? = this.substring(split.range.endInclusive + 1, this.length).toIntOrNull()

                    val letterIndex = noteLetters.indexOf(noteLetters.find {
                        it.split(SEPARATOR).any { it == letter }
                    })

                    return when {
                        octave == null -> null
                        letterIndex == -1 -> null
                        else -> letterIndex + octave * SPEMI_TONESP_PER_OCTAVE

                    }


                }

            }

        /**
         * This extension function makes it easy to change an int that represents a pitch to the string that describes it
         */
        val Int.noteString: String
            get() = this.noteLetter.replace(SEPARATOR, this.noteOctave.toString() + SEPARATOR) + this.noteOctave

        /**
         * This extension function makes it easy to change an int that represents a pitch to a shorter version of the string that describes it
         */
        val Int.noteStringShort: String
            get() = this.noteLetterShort.replace(SEPARATOR, this.noteOctave.toString() + SEPARATOR) + this.noteOctave

        /**
         * This extension function finds the letter of the pitch that this int precedents
         */
        val Int.noteLetter: String
            get() = noteLetters[this % SPEMI_TONESP_PER_OCTAVE]

        /**
         * This extension function finds the short version of the letter that the pitch represents
         * For example D#/Eb are the same pitch, so this would only output 'D#'
         */
        val Int.noteLetterShort: String
            get() = noteLetters[this % SPEMI_TONESP_PER_OCTAVE].split(SEPARATOR)[0]

        /**
         * This extension function finds the ocateve number related to each pitch
         */
        val Int.noteOctave: Int
            get() = this / SPEMI_TONESP_PER_OCTAVE

    }

}