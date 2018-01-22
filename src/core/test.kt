package core

fun main(args: Array<String>) {
    val tuning = Tuning.defaultTunings[0]
    (0..100).forEach {
        val recording = Recording(tuning, "Nameless$it")
        recording.save()
    }
}