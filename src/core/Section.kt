package core

/**
 * This class is for storing the sections of recording that the use can split the entire recording into
 * @see Recording
 * @property from The inclusive start of the section in time steps
 * @property to The exclusive end of the section in time steps, a value of -1 means that it goes to the end
 */
class Section(private val recording: Recording, val from: Int, val to: Int) {
    constructor(recording: Recording, range: IntRange) : this(recording, range.endInclusive, range.endInclusive - 1)

    /**
     * The length of the section in samples
     */
    val length: Int
        get() = correctedTo - from

    /**
     * The end of the section corrected for the fact that -1 means all the way to the end
     */
    val correctedTo: Int
        get() = if (to == -1) recording.length - 1 else to

    override fun toString(): String {
        return "Section(recording=$recording, from=$from, to=$to)"
    }

    companion object {

        const val minLength = 10

    }

}