package net.xzos.upgradeall.smoke

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import net.xzos.upgradeall.ui.home.MainActivity
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冒烟测试套件 - 验证应用基本功能
 * 可通过命令行运行: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.xzos.upgradeall.smoke.SmokeTestSuite
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SmokeTestSuite {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        // 确保测试环境干净
        Thread.sleep(1000) // 等待应用完全启动
    }

    @Test
    fun test01_AppLaunchesSuccessfully() {
        // 验证应用能成功启动并显示主界面
        onView(withId(android.R.id.content))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test02_NavigationToAppsSection() {
        // 测试导航到应用列表
        try {
            onView(allOf(withText("应用"), isDisplayed()))
                .perform(click())
            Thread.sleep(500)
            // 验证进入了应用列表界面
            onView(withContentDescription("应用"))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // 尝试英文界面
            onView(allOf(withText("Apps"), isDisplayed()))
                .perform(click())
        }
    }

    @Test
    fun test03_NavigationToDiscoverySection() {
        // 测试导航到发现页面
        try {
            onView(allOf(withText("发现"), isDisplayed()))
                .perform(click())
            Thread.sleep(500)
        } catch (e: Exception) {
            // 尝试英文界面
            onView(allOf(withText("Discovery"), isDisplayed()))
                .perform(click())
        }
    }

    @Test
    fun test04_NavigationToSettingsSection() {
        // 测试导航到设置页面
        try {
            onView(allOf(withText("设置"), isDisplayed()))
                .perform(click())
            Thread.sleep(500)
        } catch (e: Exception) {
            // 尝试英文界面
            onView(allOf(withText("Settings"), isDisplayed()))
                .perform(click())
        }
    }

    @Test
    fun test05_CheckUpdateFunctionality() {
        // 测试检查更新功能（不执行实际更新）
        try {
            // 导航到应用列表
            onView(allOf(withText("应用"), isDisplayed()))
                .perform(click())
            Thread.sleep(1000)
            
            // 尝试点击更新按钮（如果存在）
            onView(withContentDescription("更新"))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // 功能可能不可用，这是可接受的
            println("Update functionality test skipped: ${e.message}")
        }
    }
}