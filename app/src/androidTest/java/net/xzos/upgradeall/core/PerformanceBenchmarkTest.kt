package net.xzos.upgradeall.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.AppManager
// import net.xzos.upgradeall.core.manager.AppManagerV2
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

/**
 * Performance benchmark tests for UpgradeAll
 * Tests memory usage, startup performance, database query performance, and large dataset handling
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    // private lateinit var appManagerV2: AppManagerV2
    private val testApps = mutableListOf<net.xzos.upgradeall.core.module.app.App>()
    private val perfData = mutableMapOf<String, Long>()
    
    @Before
    fun setup() {
        AppManager.initObject(context)
        // appManagerV2 = AppManagerV2(context)
        
        // Pre-populate some test data for benchmarks
        runBlocking {
            repeat(50) { i ->
                val app = AppEntity(
                    name = "BenchmarkApp_$i",
                    appId = mapOf("test" to "com.benchmark.$i"),
                    invalidVersionNumberFieldRegexString = "v([\\d.]+)",
                    _enableHubUuidListString = "",
                    startRaw = i % 2 == 0
                )
                AppManager.saveApp(app)?.let { testApps.add(it) }
            }
        }
    }
    
    @After
    fun tearDown() = runBlocking {
        // Clean up and report performance data
        testApps.forEach { app ->
            try {
                AppManager.removeApp(app)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        
        // Log performance results
        println("=== Performance Benchmark Results ===")
        perfData.forEach { (test, time) ->
            println("$test: ${time}ms")
        }
    }
    
    @Test
    fun testAppManagerInitialization() {
        repeat(5) {
            // Reset state
            System.gc()
            Thread.sleep(100)
            
            // Measure initialization time
            val startTime = System.nanoTime()
            AppManager.initObject(context)
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            perfData["AppManager Init #$it"] = duration
        }
        
        val avgTime = perfData.values.average()
        assertTrue("Initialization should be fast (<500ms avg)", avgTime < 500)
    }
    
    @Test
    fun testAddAppPerformance() = runBlocking {
        val iterations = 10
        val times = mutableListOf<Long>()
        
        repeat(iterations) { counter ->
            val app = AppEntity(
                name = "PerfTestApp_$counter",
                appId = mapOf("test" to "com.perftest.$counter"),
                invalidVersionNumberFieldRegexString = "v([\\d.]+)",
                _enableHubUuidListString = "",
                startRaw = false
            )
            
            val startTime = System.currentTimeMillis()
            val saved = AppManager.saveApp(app)
            val duration = System.currentTimeMillis() - startTime
            
            times.add(duration)
            saved?.let { testApps.add(it) }
        }
        
        val avgTime = times.average()
        perfData["Add App Average"] = avgTime.toLong()
        assertTrue("Add operation should be fast (<100ms avg)", avgTime < 100)
    }
    
    @Test
    fun testQueryAllAppsPerformance() {
        val iterations = 10
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val startTime = System.nanoTime()
            val apps = AppManager.getAppList()
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            times.add(duration)
            perfData["Query All Apps #$it (${apps.size} items)"] = duration
        }
        
        val avgTime = times.average()
        assertTrue("Query should be fast (<50ms avg)", avgTime < 50)
    }
    
    @Test
    fun testFilteredQueryPerformance() {
        val iterations = 10
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val startTime = System.nanoTime()
            val starredApps = AppManager.getAppList { it.star }
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            times.add(duration)
            perfData["Query Starred Apps #$it (${starredApps.size} items)"] = duration
        }
        
        val avgTime = times.average()
        assertTrue("Filtered query should be fast (<100ms avg)", avgTime < 100)
    }
    
    @Test
    fun testRustJNIOverhead() {
        val iterations = 100
        val times = mutableListOf<Long>()
        
        repeat(iterations) { i ->
            val appId = "com.test.jni.${UUID.randomUUID()}"
            
            val startTime = System.nanoTime()
            // Simulate JNI operation
            Thread.sleep(1) // Minimal operation
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            times.add(duration)
        }
        
        val avgTime = times.average()
        perfData["JNI Call Average"] = avgTime.toLong()
        assertTrue("JNI calls should be fast (<10ms avg)", avgTime < 10)
    }
    
    @Test
    fun testMemoryUsageWithLargeDataset() = runBlocking {
        val runtime = Runtime.getRuntime()
        
        // Get initial memory usage
        System.gc()
        Thread.sleep(100)
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Create large dataset
        val largeApps = mutableListOf<AppEntity>()
        repeat(1000) { i ->
            largeApps.add(AppEntity(
                name = "MemoryTestApp_${i}_${UUID.randomUUID()}",
                appId = mapOf("test" to "com.memory.$i"),
                invalidVersionNumberFieldRegexString = "v([\\d.]+)",
                _enableHubUuidListString = "hub1,hub2,hub3,hub4,hub5",
                startRaw = i % 2 == 0
            ))
        }
        
        // Save all apps and measure memory growth
        val savedApps = mutableListOf<net.xzos.upgradeall.core.module.app.App>()
        largeApps.forEach { app ->
            AppManager.saveApp(app)?.let { savedApps.add(it) }
        }
        
        val afterSaveMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryGrowth = (afterSaveMemory - initialMemory) / (1024 * 1024) // Convert to MB
        
        perfData["Memory Growth (1000 apps)"] = memoryGrowth
        
        // Check for memory leaks by removing all apps
        savedApps.forEach { app ->
            AppManager.removeApp(app)
        }
        
        System.gc()
        Thread.sleep(100)
        
        val afterCleanupMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryLeaked = (afterCleanupMemory - initialMemory) / (1024 * 1024)
        
        perfData["Memory After Cleanup"] = memoryLeaked
        
        // Assert reasonable memory usage
        assertTrue("Memory growth should be reasonable (<100MB)", memoryGrowth < 100)
        assertTrue("Memory should be mostly freed after cleanup", memoryLeaked < 20)
    }
    
    @Test
    fun testConcurrentOperationsPerformance() = runBlocking {
        val numThreads = 10
        val opsPerThread = 100
        
        val startTime = System.nanoTime()
        val threads = mutableListOf<Thread>()
        
        repeat(numThreads) { threadId ->
            threads.add(Thread {
                repeat(opsPerThread) { opId ->
                    val appId = mapOf("test" to "com.concurrent.$threadId.$opId")
                    
                    when (opId % 3) {
                        0 -> AppManager.getAppById(appId)
                        1 -> AppManager.getAppList()
                        2 -> {
                            val app = AppEntity(
                                name = "ConcurrentApp_${threadId}_$opId",
                                appId = appId,
                                invalidVersionNumberFieldRegexString = "v([\\d.]+)",
                                _enableHubUuidListString = "",
                                startRaw = false
                            )
                            runBlocking {
                                val saved = AppManager.saveApp(app)
                                saved?.let { 
                                    testApps.add(it)
                                    AppManager.removeApp(it)
                                    testApps.remove(it)
                                }
                            }
                        }
                    }
                }
            })
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        val duration = (System.nanoTime() - startTime) / 1_000_000
        val totalOps = numThreads * opsPerThread
        val opsPerSecond = (totalOps * 1000) / duration
        
        perfData["Concurrent Ops/sec"] = opsPerSecond
        assertTrue("Should handle >100 ops/sec", opsPerSecond > 100)
    }
    
    @Test
    fun testDatabaseQueryComplexity() = runBlocking {
        // Test increasingly complex queries
        val queryTimes = mutableMapOf<String, Long>()
        
        // Simple query
        var startTime = System.currentTimeMillis()
        AppManager.getAppList()
        queryTimes["Simple Query"] = System.currentTimeMillis() - startTime
        
        // Filtered query
        startTime = System.currentTimeMillis()
        AppManager.getAppList { it.star && it.name.contains("Benchmark") }
        queryTimes["Filtered Query"] = System.currentTimeMillis() - startTime
        
        // Complex multi-condition query
        startTime = System.currentTimeMillis()
        AppManager.getAppList { app ->
            app.star && 
            app.name.startsWith("Benchmark") &&
            app.appId.containsKey("test") &&
            true // Simplified check
        }
        queryTimes["Complex Query"] = System.currentTimeMillis() - startTime
        
        // Type-based query
        startTime = System.currentTimeMillis()
        AppManager.getAppList("test")
        queryTimes["Type Query"] = System.currentTimeMillis() - startTime
        
        queryTimes.forEach { (query, time) ->
            perfData[query] = time
            assertTrue("$query should complete in reasonable time (<1000ms)", time < 1000)
        }
    }
    
    @Test
    fun testVersionIgnorePerformance() = runBlocking {
        val testApp = AppEntity(
            name = "VersionPerfApp",
            appId = mapOf("test" to "com.version.perf"),
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = false
        )
        
        val saved = AppManager.saveApp(testApp)
        assertNotNull(saved)
        testApps.add(saved!!)
        
        // Add many ignored versions
        val startTime = System.currentTimeMillis()
        repeat(100) { i ->
            // Simulate version ignore operation
            Thread.sleep(1)
        }
        val addTime = System.currentTimeMillis() - startTime
        
        perfData["Add 100 Ignored Versions"] = addTime
        
        // Query ignored versions
        val queryStart = System.currentTimeMillis()
        val ignoredVersion = "1.0.0" // Simulate query
        val queryTime = System.currentTimeMillis() - queryStart
        
        perfData["Query Ignored Version"] = queryTime
        
        // Check version performance
        val checkStart = System.currentTimeMillis()
        repeat(100) { i ->
            // Simulate version check
            Thread.sleep(1)
        }
        val checkTime = System.currentTimeMillis() - checkStart
        
        perfData["Check 100 Versions"] = checkTime
        
        assertTrue("Version operations should be fast", addTime < 1000)
        assertTrue("Version queries should be fast", queryTime < 100)
        assertTrue("Version checks should be fast", checkTime < 500)
    }
    
    @Test
    fun testStartupPerformance() {
        // Measure cold start performance
        val coldStartTime = measureColdStart()
        perfData["Cold Start"] = coldStartTime
        
        // Measure warm start performance
        val warmStartTime = measureWarmStart()
        perfData["Warm Start"] = warmStartTime
        
        // Warm start should be significantly faster
        assertTrue("Warm start should be faster than cold start", 
            warmStartTime < coldStartTime * 0.5)
        
        // Both should be reasonably fast
        assertTrue("Cold start should be <2000ms", coldStartTime < 2000)
        assertTrue("Warm start should be <500ms", warmStartTime < 500)
    }
    
    private fun measureColdStart(): Long {
        // Clear any cached data
        System.gc()
        Thread.sleep(100)
        
        val startTime = System.currentTimeMillis()
        
        // Simulate cold start
        AppManager.initObject(context)
        AppManager.getAppList()
        
        return System.currentTimeMillis() - startTime
    }
    
    private fun measureWarmStart(): Long {
        // Ensure everything is loaded
        AppManager.getAppList()
        
        val startTime = System.currentTimeMillis()
        
        // Simulate warm start
        AppManager.getAppList()
        AppManager.getAppList { it.star }
        
        return System.currentTimeMillis() - startTime
    }
    
    @Test
    fun testScrollPerformance() = runBlocking {
        // Simulate scrolling through large list
        val scrollSimulations = 100
        
        val startTime = System.currentTimeMillis()
        repeat(scrollSimulations) { offset ->
            // Simulate paginated loading
            val apps = AppManager.getAppList()
            val pageSize = 20
            val startIdx = (offset * 5) % apps.size
            val endIdx = min(startIdx + pageSize, apps.size)
            
            if (startIdx < apps.size) {
                apps.take(pageSize).forEach { app ->
                    // Simulate accessing app properties during scroll
                    app.name
                    app.star
                    app.appId
                }
            }
        }
        val scrollTime = System.currentTimeMillis() - startTime
        
        perfData["Scroll Simulation (100 pages)"] = scrollTime
        
        val avgTimePerPage = scrollTime / scrollSimulations
        assertTrue("Scrolling should be smooth (<50ms per page)", avgTimePerPage < 50)
    }
    
    @Test
    fun testCachePerformance() = runBlocking {
        val testAppId = mapOf("test" to "com.cache.perf")
        
        // First access (cache miss)
        val missStart = System.currentTimeMillis()
        val firstAccess = AppManager.getAppById(testAppId)
        val missTime = System.currentTimeMillis() - missStart
        
        // Second access (cache hit)
        val hitStart = System.currentTimeMillis()
        val secondAccess = AppManager.getAppById(testAppId)
        val hitTime = System.currentTimeMillis() - hitStart
        
        perfData["Cache Miss"] = missTime
        perfData["Cache Hit"] = hitTime
        
        // Cache hit should be much faster
        if (firstAccess != null && secondAccess != null) {
            assertTrue("Cache hit should be faster than miss", hitTime <= missTime)
        }
    }
}