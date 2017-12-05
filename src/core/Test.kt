package core

fun main(args: Array<String>) {

    val note = "ABC123"

    val split = "[a-zA-Z]\\d".toRegex().find(note)!!

    val letter: String = note.substring(0, split.range.start + 1)
    val octave = note.substring(split.range.start + 1, note.length)

    println(letter)
    println(octave)


    val tuning = Tuning("E2", "A2", "D3", "G3", "B3", "E4")

    for (i in 0..80) {

        println("$i\t${i.noteString.split("/")[0]}\t${i in tuning}")

    }

    val melody = listOf(
            "E2".pitch + 1,
            "E2".pitch + 3,
            "A2".pitch + 1,
            "E2".pitch + 3,

            "E2".pitch + 1,
            "E2".pitch + 3,
            "A2".pitch + 1,
            "D3".pitch + 1,
            "D3".pitch + 3,
            "D3".pitch + 1,

            "A2".pitch + 3,
            "A2".pitch + 5,
            "A2".pitch + 3,
            "E2".pitch + 5,

            "A2".pitch + 3,
            "A2".pitch + 5,
            "A2".pitch + 3,
            "A2".pitch + 5,
            "E2".pitch + 3
    )


}
