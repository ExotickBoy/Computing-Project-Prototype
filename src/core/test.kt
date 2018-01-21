package core


fun main(args: Array<String>) {

    val b: Int? = 2

    print(b?.a)

}

val Int.a: Int get() = 2