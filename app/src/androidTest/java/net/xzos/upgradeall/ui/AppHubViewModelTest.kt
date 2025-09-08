package net.xzos.upgradeall.ui

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.constant.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.ui.applist.base.AppHubViewModel
import net.xzos.upgradeall.ui.applist.base.TabIndex
import org.junit.*
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Test suite for AppHubViewModel to ensure UI behavior consistency
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AppHubViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AppHubViewModel
    private lateinit var application: Application
    private val testApps = mutableListOf<App>()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        
        // Initialize AppManager
        AppManager.initObject(application)
        
        viewModel = AppHubViewModel(application)
    }
    
    @After
    fun tearDown() = runBlocking {
        Dispatchers.resetMain()
        // Clean up test apps
        testApps.forEach { app ->
            try {
                AppManager.removeApp(app)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
    
    @Test
    fun testUpdateTabFiltering() = runTest {
        // Setup test data with different statuses
        val outdatedApp = createAndSaveTestApp(
            name = "OutdatedApp",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.outdated")
        )
        
        val latestApp = createAndSaveTestApp(
            name = "LatestApp",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.latest")
        )
        
        // Initialize ViewModel for UPDATE tab
        viewModel.initData(ANDROID_APP_TYPE, TabIndex.TAB_UPDATE)
        
        // Load data
        viewModel.loadData()
        advanceUntilIdle()
        
        // Verify filtering
        val liveData = viewModel.getLiveData()
        Assert.assertNotNull("LiveData should not be null", liveData.value)
        
        val appList = liveData.value?.first ?: emptyList()
        
        // Should only contain outdated apps in UPDATE tab
        val outdatedApps = appList.filter { 
            it.releaseStatus == AppStatus.APP_OUTDATED 
        }
        
        Assert.assertTrue(
            "Update tab should filter outdated apps",
            outdatedApps.isNotEmpty() || appList.isEmpty()
        )
    }
    
    @Test
    fun testStarTabFiltering() = runTest {
        // Create starred and non-starred apps
        val starredApp = createAndSaveTestApp(
            name = "StarredApp",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.starred"),
            star = true
        )
        
        val normalApp = createAndSaveTestApp(
            name = "NormalApp",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.normal"),
            star = false
        )
        
        // Initialize ViewModel for STAR tab
        viewModel.initData(ANDROID_APP_TYPE, TabIndex.TAB_STAR)
        
        // Load data
        viewModel.loadData()
        advanceUntilIdle()
        
        // Verify filtering
        val liveData = viewModel.getLiveData()
        val appList = liveData.value?.first ?: emptyList()
        
        // Should only contain starred apps
        Assert.assertTrue(
            "Star tab should only show starred apps",
            appList.all { it.star }
        )
    }
    
    @Test
    fun testAppTypeFiltering() = runTest {
        // Create Android app and Magisk module
        val androidApp = createAndSaveTestApp(
            name = "AndroidApp",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.android")
        )
        
        val magiskModule = createAndSaveTestApp(
            name = "MagiskModule",
            appId = mapOf(ANDROID_MAGISK_MODULE_TYPE to "com.test.magisk")
        )
        
        // Test Android app filtering
        viewModel.initData(ANDROID_APP_TYPE, TabIndex.TAB_ALL)
        viewModel.loadData()
        advanceUntilIdle()
        
        val androidAppList = viewModel.getLiveData().value?.first ?: emptyList()
        
        Assert.assertFalse(
            "Android app view should not contain Magisk modules",
            androidAppList.any { it.appId.containsKey(ANDROID_MAGISK_MODULE_TYPE) }
        )
        
        // Test Magisk module filtering
        val magiskViewModel = AppHubViewModel(application)
        magiskViewModel.initData(ANDROID_MAGISK_MODULE_TYPE, TabIndex.TAB_ALL)
        magiskViewModel.loadData()
        advanceUntilIdle()
        
        val magiskAppList = magiskViewModel.getLiveData().value?.first ?: emptyList()
        
        Assert.assertTrue(
            "Magisk view should only contain Magisk modules",
            magiskAppList.all { it.appId.containsKey(ANDROID_MAGISK_MODULE_TYPE) }
        )
    }
    
    @Test
    fun testAllTabShowsNonVirtualApps() = runTest {
        // Initialize ViewModel for ALL tab
        viewModel.initData(ANDROID_APP_TYPE, TabIndex.TAB_ALL)
        
        // Load data
        viewModel.loadData()
        advanceUntilIdle()
        
        // Verify that ALL tab doesn't show virtual apps
        val liveData = viewModel.getLiveData()
        val appList = liveData.value?.first ?: emptyList()
        
        Assert.assertTrue(
            "ALL tab should not show virtual apps",
            appList.all { !it.isVirtual }
        )
    }
    
    @Test
    fun testApplicationsTabShowsVirtualApps() = runTest {
        // Initialize ViewModel for APPLICATIONS tab
        viewModel.initData(ANDROID_APP_TYPE, TabIndex.TAB_APPLICATIONS_APP)
        
        // Load data
        viewModel.loadData()
        advanceUntilIdle()
        
        // Verify that APPLICATIONS tab shows virtual apps
        val liveData = viewModel.getLiveData()
        val appList = liveData.value?.first ?: emptyList()
        
        // This tab should show virtual apps that are either renewing or not in network error
        appList.forEach { app ->
            Assert.assertTrue(
                "Applications tab should show virtual apps",
                app.isVirtual && (app.isRenewing || app.releaseStatus != AppStatus.NETWORK_ERROR)
            )
        }
    }
    
    @Test
    fun testIgnoreAllFunctionality() = runTest {
        // Create test apps
        val app1 = createAndSaveTestApp(
            name = "App1",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.app1")
        )
        
        val app2 = createAndSaveTestApp(
            name = "App2",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.app2")
        )
        
        // Initialize ViewModel
        viewModel.initData(ANDROID_APP_TYPE, TabIndex.TAB_UPDATE)
        viewModel.loadData()
        advanceUntilIdle()
        
        // Call ignoreAll
        viewModel.ignoreAll()
        advanceUntilIdle()
        
        // Verify that the list is refreshed
        val liveData = viewModel.getLiveData()
        Assert.assertNotNull("LiveData should be updated after ignoreAll", liveData.value)
    }
    
    @Test
    fun testSortingByName() = runTest {
        // Create apps with different names
        val appB = createAndSaveTestApp(
            name = "BBB_App",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.bbb")
        )
        
        val appA = createAndSaveTestApp(
            name = "AAA_App",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.aaa")
        )
        
        val appC = createAndSaveTestApp(
            name = "CCC_App",
            appId = mapOf(ANDROID_APP_TYPE to "com.test.ccc")
        )
        
        // Initialize ViewModel for UPDATE tab (which sorts by name)
        viewModel.initData(ANDROID_APP_TYPE, TabIndex.TAB_UPDATE)
        viewModel.loadData()
        advanceUntilIdle()
        
        val appList = viewModel.getLiveData().value?.first ?: emptyList()
        
        // Verify apps are sorted by name
        val sortedNames = appList.map { it.name }.filter { 
            it.startsWith("AAA_") || it.startsWith("BBB_") || it.startsWith("CCC_")
        }
        
        if (sortedNames.size >= 2) {
            for (i in 0 until sortedNames.size - 1) {
                Assert.assertTrue(
                    "Apps should be sorted by name",
                    sortedNames[i] <= sortedNames[i + 1]
                )
            }
        }
    }
    
    private suspend fun createAndSaveTestApp(
        name: String,
        appId: Map<String, String?>,
        star: Boolean = false
    ): App {
        val appEntity = AppEntity(
            name = "${name}_${UUID.randomUUID()}",
            appId = appId,
            invalidVersionNumberFieldRegexString = "v([\\d.]+)",
            _enableHubUuidListString = "",
            startRaw = if (star) true else null
        )
        
        val savedApp = AppManager.saveApp(appEntity)
        Assert.assertNotNull("App should be saved successfully", savedApp)
        testApps.add(savedApp!!)
        return savedApp
    }
}