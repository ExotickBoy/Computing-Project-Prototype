package core

import java.io.Serializable

enum class ChordPattern(val suffix: String, val notes: List<Int>, val maxStringSep: Int? = null) : Serializable {

    MAJOR("M", 0, 4, 7),
    MINOR("m", 0, 3, 7),
    DIMINISHED("d", 0, 3, 6),
    AUGMENTED("a", 0, 4, 8),
    POWER_CHORD("+7", 0, 7, maxStringSep = 2),
    PLUS_SEVEN("+7", 0, 7, 12, maxStringSep = 3),
    PLUS_FIVE("+5", 0, 5, maxStringSep = 2);

    constructor(suffix: String, vararg notes: Int, maxStringSep: Int? = null) : this(suffix, notes.toList(), maxStringSep)

}