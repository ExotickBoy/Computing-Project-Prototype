package core

class Recording(val tuning: Tuning, val name: String) {

    val timeSteps: MutableList<TimeStep> = mutableListOf()
    val placements = mutableListOf<Placement>()
    val notes = mutableListOf<Note>()

    val paths: MutableList<List<Path>> = mutableListOf()
    val possiblePlacements: MutableList<List<Placement>> = mutableListOf()

    fun addTimeStep(timeStep: TimeStep) {

        synchronized(possiblePlacements) {

            timeSteps.add(timeStep)

            timeStep.notes.filter { !notes.contains(it) }
                    .filter { it.pitch in tuning } // the pitch must be within the playable range of the guitar
                    .forEach {
                        notes.add(it)
                        possiblePlacements.add(findPlacements(it, tuning))
                    }

            optimiseForward(0)

        }

    }

    private fun optimiseForward(from: Int) {

        for (time in from until possiblePlacements.size) {

            val currentPlacements = possiblePlacements[time]

            val nextPaths = if (time == 0) { // no previous paths

                (0 until currentPlacements.size).map {
                    Path(listOf(it), currentPlacements[it].startDistance())
                }

            } else {

                val previousPaths = paths[time - 1]
                (0 until currentPlacements.size).map { current ->

                    (0 until previousPaths.size).map { past ->

                        Path(previousPaths[past].route + current,
                                previousPaths[past].route.mapIndexed { index, place ->
                                    possiblePlacements[index][place] distance currentPlacements[current]
                                }.takeLast(10).sum())

                    }.minBy { it.distance }!!

                }

            }

            if (time < paths.size) {
                paths[time] = nextPaths
            } else {
                paths.add(nextPaths)
            }

        }

        if (paths.size > 0) {

            val bestPath = paths[paths.size - 1].minBy { it.distance }?.route!!

            (from until possiblePlacements.size).forEach {
                val currentPlacement = possiblePlacements[it][bestPath[it]]

                if (it < placements.size) {
                    placements[it] = currentPlacement
                } else {
                    placements.add(currentPlacement)
                }
            }

        }

//        println("${possiblePlacements.size}\t${placements.size}")

    }

    private fun optimiseForward(note: Note) {

        val currentPlacements = possiblePlacements[possiblePlacements.size - 1]

        if (paths.size == 0) {

            paths.add((0 until currentPlacements.size).map {
                Path(listOf(it), 0)
            })

        } else {

            val previousPaths = paths[paths.size - 1]

            val nextPaths = (0 until currentPlacements.size).map { current ->

                (0 until previousPaths.size).map { past ->

                    Path(
                            previousPaths[past].route + current,
                            previousPaths[past].route.mapIndexed { index, place ->
                                possiblePlacements[index][place] distance currentPlacements[current]
                            }.sum()
                    )

                }.minBy { it.distance }!!

            }
            paths.add(nextPaths)

            placements.clear()
            placements.addAll(nextPaths.minBy { it.distance }?.route?.mapIndexed { index, it -> possiblePlacements[index][it] } ?: mutableListOf())


        }

    }

    companion object {

        fun findPlacements(note: Note, tuning: Tuning): List<Placement> {

            return tuning.strings.mapIndexed { index, it ->
                Placement(note.pitch - it, index, note)
            }.filter {
                it.fret >= 0 && it.fret <= tuning.maxFret
            }

        }

    }

    data class Path(val route: List<Int>, val distance: Double) {
        constructor(route: List<Int>, distance: Number) : this(route, distance.toDouble())
    }

}