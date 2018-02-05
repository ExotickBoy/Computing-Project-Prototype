package core

import java.io.Serializable

enum class ChordPattern(val suffix: String, val notes: List<Int>, val maxStringSpan: Int? = null) : Serializable {

    MAJOR("M", 0, 4, 7),
    MINOR("m", 0, 3, 7),
    DIMINISHED("d", 0, 3, 6),
    AUGMENTED("a", 0, 4, 8),
    SIMPLE_POWER_CHORD("5", 0, 7, maxStringSep = 2),
    POWER_CHORD("5", 0, 7, 12, maxStringSep = 3);

    constructor(suffix: String, vararg notes: Int, maxStringSep: Int? = null) : this(suffix, notes.toList(), maxStringSep)

}