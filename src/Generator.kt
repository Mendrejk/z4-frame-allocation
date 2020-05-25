fun generateReferences(pageCount: Int, processCount: Int) {
}

fun generateProcesses(howMany: Int, pageCount: Int): List<Process> {
    // will break if amount of pages is not a few times bigger than amount of processes!
    // distributes pages between processes by finding (howMany - 1) random integers in pageCount and sorting them
    // the result is the distribution expressed in numerical ranges
    // to make sure that each process has at least 2 pages I subtract 2 * howMany from pageCount and give each process
    // 2 pages as a base
    val generationMaximum: Int = pageCount - 2 * howMany
    val divisionPoints: MutableList<Int> = mutableListOf()
    while (divisionPoints.size < howMany - 1) {
        val randomDivisionPoint: Int = (0..generationMaximum).random()
        if (randomDivisionPoint !in divisionPoints) divisionPoints.add(randomDivisionPoint)
    }
    divisionPoints.sort()
    // it's fine if it's equal to largest element in divisionPoints, the last range will just have 2 pages then
    divisionPoints.add(generationMaximum)

    val processes: MutableList<Process> = mutableListOf()
    // the 1st process has pages 0, firstDivisionPoint + 2 (the default 2 per process)
    processes.add(Process(0, divisionPoints[0] + 2))
    for (i: Int in 1 until (divisionPoints.size)) {
        processes.add(Process(
            firstPage = processes[i - 1].pages.last() + 1,
            lastPage = processes[i - 1].pages.last() + 2 + divisionPoints[i] - divisionPoints[i - 1]
        ))
    }
    return processes.toList()
}