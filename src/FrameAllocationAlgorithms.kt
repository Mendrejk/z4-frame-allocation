fun equalDistribution(processes: List<Process>, references: List<Int>, frameCount: Int): List<Int> {
    // give each process an equal amount of frames
    val framesPerProcess: Int = frameCount / processes.size
    processes.forEach { it.addFrameCapacity(framesPerProcess) }
    // some frames may be left due to conversion errors
    var framesLeft: Int = frameCount - framesPerProcess * processes.size
    if (framesLeft > 0) {
        for (process: Process in processes) {
            process.addFrameCapacity(1)
            framesLeft--
            if (framesLeft == 0) break
        }
    }

    references.withIndex().forEach() { (index: Int, reference: Int) ->
        val itProcess: Process = findProcess(processes, reference)
        if (!itProcess.hasReference(reference)) {
            if (itProcess.isFrameMapFull()) {
                itProcess.removeLeastRecentlyUsedReference()
            }
            itProcess.incrementFaultCount()
        }
        // this either adds a new reference to the map, or refreshes it's last use
        itProcess.addReference(reference, index)
    }
    val pageFaultCounts: List<Int> = List(processes.size) { processes[it].pageFaultCount }
    // reset all processes
    processes.forEach { it.reset() }
    return pageFaultCounts
}

private fun findProcess(processes: List<Process>, page: Int): Process = processes.first { it.hasPage(page) }