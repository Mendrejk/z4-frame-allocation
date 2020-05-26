fun main() {
    // a process generates roughly 1000 references for each page it has got, so 100 pages generate +- 100000 references
    val processes: List<Process> = generateProcesses(PROCESS_COUNT, PAGE_COUNT)
    print("Process' page counts: ")
    processes.forEach { print("${it.pages.size} ") }
    val references: List<Int> = generateReferences(processes)
    // give copy of processes as arguments for algorithms
    println("\n---------------------------------------------------")
    val resultsEqualDistribution: List<Int> = equalDistribution(processes, references, FRAME_COUNT)
    println("equal distribution: $resultsEqualDistribution, sum: ${resultsEqualDistribution.sum()}")
    val resultsProportionalDistribution: List<Int> = proportionalDistribution(processes, references, FRAME_COUNT)
    println("proportional distribution: $resultsProportionalDistribution, sum: ${resultsProportionalDistribution.sum()}")
    val resultsPageFaultFrequency: Pair<List<Int>, Int> = pageFaultFrequency(processes, references, FRAME_COUNT)
    println("page fault frequency: ${resultsPageFaultFrequency.first}, sum: ${resultsPageFaultFrequency.first.sum()}," +
            " thrashing count: ${resultsPageFaultFrequency.second}")
}