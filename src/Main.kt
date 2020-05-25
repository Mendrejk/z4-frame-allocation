fun main() {
    // a process generates on average 1000 references for each page it has got
    val processes: List<Process> = generateProcesses(10, 100)
    processes.forEach { println(it.pages) }
    val references: List<Int> = generateReferences(processes)
}