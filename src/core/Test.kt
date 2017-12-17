package core

import kotlin.math.pow

fun main(args: Array<String>) {

    println("c1".pitch.frequency)

    val tuning = Tuning("E2", "A2", "D3", "G3", "B3", "E4")
    println(tuning.strings)

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
    println(melody)
    for (i in 0 until 100) {
        println("starting")
//        melody.takeLast(i).forEachIndexed { ind, i -> println("$ind\t $i") }
    }


}

private val Int.frequency: Double
    get() {
        return 27.5 * (2.0.pow(1 / 12.0)).pow(this - 9)
    }
