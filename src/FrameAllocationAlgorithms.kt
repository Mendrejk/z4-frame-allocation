import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

fun proportionalDistribution(processes: List<Process>, references: List<Int>, frameCount: Int): List<Int> {
    // give each process amount of frames proportional to it's share of pages, but at least one each
    val pagesTotal: Int = PAGE_COUNT
    var givenOutFrames: Int = 0
    processes.forEach {
        val currentFrames: Int = max((frameCount * it.pages.size / pagesTotal.toDouble()).roundToInt(), 1)
        it.addFrameCapacity(currentFrames)
        givenOutFrames += currentFrames
    }
    // due to rounding with exact halves it's possible that too many frames were given out
    while (givenOutFrames > frameCount) {
        val randomProcess: Process = processes.random()
        if (randomProcess.frameMapCapacity > 1) {
            randomProcess.decreaseFrameCapacity(1)
            givenOutFrames--
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

fun pageFaultFrequency(processes: List<Process>, references: List<Int>, frameCount: Int): Pair<List<Int>, Int> {
    // start with proportional distribution
    val pagesTotal: Int = PAGE_COUNT
    var givenOutFrames: Int = 0
    processes.forEach {
        val currentFrames: Int = max((frameCount * it.pages.size / pagesTotal.toDouble()).roundToInt(), 1)
        it.addFrameCapacity(currentFrames)
        givenOutFrames += currentFrames
    }
    while (givenOutFrames > frameCount) {
        val randomProcess: Process = processes.random()
        if (randomProcess.frameMapCapacity > 1) {
            randomProcess.decreaseFrameCapacity(1)
            givenOutFrames--
        }
    }
    // create originalDistribution for later process resuming
    val originalDistribution: MutableList<Int> = mutableListOf()
    processes.forEach { originalDistribution.add(it.frameMapCapacity) }

    val calculationTime = PFF_CALCULATION_TIME
    var freeFrames: Int = 0
    var thrashingCount: Int = 0

    references.withIndex().forEach() { (index: Int, reference: Int) ->
        if (index % calculationTime == 0 && index != 0) {
            processes.withIndex().forEach { (index: Int, it: Process) ->
                val pff: Double = it.calculatePff()
                // resume process if it can receive it's proportional amount of frames
                when {
                    it.isFrozen -> {
                        if (freeFrames >= originalDistribution[index]) {
                            it.resume(originalDistribution[index])
                            freeFrames -= originalDistribution[index]
                        }
                    }
                    pff <= PFF_LOW -> {
                        // don't decrease frames below 1
                        if (it.frameMapCapacity > 1) {
                            it.decreaseFrameCapacity(1)
                            freeFrames++
                        }
                    }
                    pff >= PFF_HIGH -> {
                        if (freeFrames > 0) {
                            freeFrames--
                            it.addFrameCapacity(1)
                        } else if (pff >= PFF_CRITICAL) {
                            freeFrames += it.freeze()
                            thrashingCount++
                        }
                    }
                }
            }
            // clear pff values
            processes.forEach { it.resetPFF() }
        }

        val itProcess: Process = findProcess(processes, reference)
        if (itProcess.isFrozen) {
            itProcess.frozenQueue.add(reference)
        } else {
            itProcess.incrementPffReferences()
            if (!itProcess.hasReference(reference)) {
                if (itProcess.isFrameMapFull()) {
                    itProcess.removeLeastRecentlyUsedReference()
                }
                itProcess.incrementFaultCount()
                itProcess.incrementPffPageFaults()
            }
            // this either adds a new reference to the map, or refreshes it's last use
            itProcess.addReference(reference, index)
        }
    }
    val pageFaultCounts: List<Int> = List(processes.size) { processes[it].pageFaultCount }
    // reset all processes
    processes.forEach { it.reset() }
    return Pair(pageFaultCounts, thrashingCount)
}

fun zoneBasedAllocation(processes: List<Process>, references: List<Int>, frameCount: Int): List<Int> {
    // start with proportional distribution
    val pagesTotal: Int = PAGE_COUNT
    var givenOutFrames: Int = 0
    processes.forEach {
        val currentFrames: Int = max((frameCount * it.pages.size / pagesTotal.toDouble()).roundToInt(), 1)
        it.addFrameCapacity(currentFrames)
        givenOutFrames += currentFrames
    }
    while (givenOutFrames > frameCount) {
        val randomProcess: Process = processes.random()
        if (randomProcess.frameMapCapacity > 1) {
            randomProcess.decreaseFrameCapacity(1)
            givenOutFrames--
        }
    }

    val calculationScope: Int = WSS_CALCULATION_SCOPE
    val calculationTime: Int = WSS_CALCULATION_TIME
    var freeFrames: Int = 0

    references.withIndex().forEach() { (index: Int, reference: Int) ->
        if (index % calculationTime == 0 && index >= calculationScope) {
            // while there are enough free frames resume frozen processes
            for (process: Process in processes) {
                if (freeFrames > 0) {
                    if (process.isFrozen && freeFrames >= process.getWSS()) {
                        process.resume(process.getWSS())
                        freeFrames -= process.getWSS()
                    }
                } else break
            }

            // no wss can be larger than frame count
            processes.forEach { if (it.getWSS() > frameCount) it.setWSS(frameCount) }

            var totalFramesNeeded: Int = processes.filter { !it.isFrozen }.map { it.getWSS() }.sum()
            // while there are not enough frames, freeze largest process
            while (totalFramesNeeded > frameCount) {
                val biggestProcess: Process = processes.filter { !it.isFrozen }.maxBy { it.frameMapCapacity }!!
                totalFramesNeeded -= biggestProcess.getWSS()
                freeFrames += biggestProcess.freeze()
            }
            // give processes their wanted amount of frames one-by-one
            for (process: Process in processes) {
                // don't allocate frames to frozen processes
                if (process.isFrozen) continue
                if (freeFrames == 0) break
                if (process.getWSS() > process.frameMapCapacity) {
                    val framesToAllocate: Int = min(freeFrames, process.getWSS() - process.frameMapCapacity)
                    process.addFrameCapacity(framesToAllocate)
                    freeFrames -= framesToAllocate
                }
            }
            // well this should not work that way
            if (index % calculationScope == 0) {
                processes.forEach { if (!it.isFrozen) it.clearWorkingSet() }
            }
        }

        val itProcess: Process = findProcess(processes, reference)
        if (itProcess.isFrozen) {
            itProcess.frozenQueue.add(reference)
        } else {
            itProcess.workingSetAdd(reference)
            if (!itProcess.hasReference(reference)) {
                if (itProcess.isFrameMapFull()) {
                    itProcess.removeLeastRecentlyUsedReference()
                }
                itProcess.incrementFaultCount()
            }
            itProcess.addReference(reference, index)
        }
    }
    processes.forEach { if (it.isFrozen) it.resume(it.getWSS()) }
    val pageFaultCounts: List<Int> = List(processes.size) { processes[it].pageFaultCount }
    // reset all processes
    processes.forEach { it.reset() }
    return pageFaultCounts
}

private fun findProcess(processes: List<Process>, page: Int): Process = processes.first { it.hasPage(page) }