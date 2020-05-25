class Process(firstPage: Int, lastPage: Int) {
    val pages: List<Int> = (firstPage..lastPage).toList()
    // takes the random values from config.kt and randomises based on them
    // this leads to each process's generator being unique
    private val localityBaseChance: Int = (0..(2 * LOCALITY_BASE_CHANCE)).random()
    private val localityGain: Int = (1..(2 * LOCALITY_GAIN)).random()
    var localityCurrentChance = localityBaseChance
        private set
    var hasLocality: Boolean = false
        private set
    private var localityReferencesLeft: Int = 0
    var localityCurrentRange: IntRange = IntRange(0, 0)
        private set
    // locality scope has to be somewhat proportional pageCount
    // by feel it seems that say 0.20 are better for bigger processes (eg. 20 out of 100 pages),
    // while bigger ones (0.40-0.45) for those small (4 out of 10 pages)
    // overall I decided that roughly 0.35 of all pages feels right
    private val localityMinimalScope: Int = (0.3 * pages.size).toInt()
    private val localityMaximalScope = (0.4 * pages.size).toInt()

    private val localityMinimalLength: Int = (LOCALITY_MINIMAL_LENGTH..(2 * LOCALITY_MINIMAL_LENGTH)).random()
    private val localityMaximalLength: Int = ((LOCALITY_MAXIMAL_LENGTH / 2)..(2 * LOCALITY_MAXIMAL_LENGTH)).random()

    fun incrementLocalityChance(): Unit {
        localityCurrentChance += localityGain
        if (localityCurrentChance > 100) localityCurrentChance = 100
    }

    fun startLocality() {
        hasLocality = true
        generateLocalityRange()
        generateLocalityLength()
    }

    fun stopLocality() {
        hasLocality = false
        localityCurrentChance = localityBaseChance
    }

    fun tickLocality(): Unit {
        localityReferencesLeft--;
    }

    fun isLocalityReferencesLeftZero(): Boolean = localityReferencesLeft == 0

    private fun generateLocalityRange(): Unit {
        val rangeLength = generateLocalityScope()
        val rangeStartIndex = (0 until (pages.size - rangeLength)).random()
        localityCurrentRange = pages[rangeStartIndex]..pages[rangeStartIndex + rangeLength]
    }

    private fun generateLocalityLength(): Unit {
        localityReferencesLeft = (localityMinimalLength..localityMaximalLength).random()
    }

    private fun generateLocalityScope(): Int = (localityMinimalScope..localityMaximalScope).random()
}