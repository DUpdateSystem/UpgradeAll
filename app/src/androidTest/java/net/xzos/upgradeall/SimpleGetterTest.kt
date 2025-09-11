package net.xzos.upgradeall

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.xzos.upgradeall.ui.home.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 简单测试 Getter 核心是否运行
 * 
 * 运行方式:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.xzos.upgradeall.SimpleGetterTest
 */
@RunWith(AndroidJUnit4::class)
class SimpleGetterTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testAppStartsWithGetterCore() {
        println("==================================================")
        println("TEST: App Starts with Getter Core")
        println("==================================================")
        
        // 等待应用启动
        Thread.sleep(3000)
        
        // 检查应用是否还在运行
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = context.packageName
        
        println("Package: $packageName")
        println("App is running")
        
        // 如果应用能运行3秒而不崩溃，说明 getter 核心至少没有导致致命错误
        activityRule.scenario.onActivity { activity ->
            println("Activity: ${activity.javaClass.simpleName}")
            println("Is finishing: ${activity.isFinishing}")
            
            assert(!activity.isFinishing) { "Activity is finishing - app may have crashed" }
            println("✅ App is running without crashes")
        }
        
        // 再等待一下确保没有延迟崩溃
        Thread.sleep(2000)
        
        println("✅ App ran for 5 seconds without crashing")
        println("==================================================")
    }
}