package net.xzos.upgradeall

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.xzos.upgradeall.ui.home.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 诊断测试 - 用于捕获应用启动失败的详细日志
 * 
 * 运行方式:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.xzos.upgradeall.DiagnosticTest
 */
@RunWith(AndroidJUnit4::class)
class DiagnosticTest {
    
    companion object {
        private const val TAG = "DiagnosticTest"
    }
    
    @Test
    fun captureAppLaunchFailure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        println("==================================================")
        println("DIAGNOSTIC TEST - App Launch")
        println("==================================================")
        println("Package: ${context.packageName}")
        println("App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
        println("Android SDK: ${android.os.Build.VERSION.SDK_INT}")
        println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        println("==================================================")
        
        // 清空 logcat
        try {
            Runtime.getRuntime().exec("logcat -c")
            Thread.sleep(100)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logcat", e)
        }
        
        // 尝试启动应用
        try {
            println("\n>>> Attempting to launch MainActivity...")
            
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            
            // 等待一下让应用完全启动
            Thread.sleep(2000)
            
            println("✅ App launched successfully!")
            
            scenario.onActivity { activity ->
                println("Activity state: ${activity.lifecycle.currentState}")
                println("Activity class: ${activity.javaClass.name}")
            }
            
            scenario.close()
            
        } catch (e: Throwable) {
            println("❌ App launch failed!")
            println("Exception type: ${e.javaClass.name}")
            println("Error message: ${e.message}")
            println("\nStack trace:")
            e.printStackTrace()
            
            // 捕获 logcat 输出
            println("\n==================================================")
            println("LOGCAT OUTPUT (last 200 lines):")
            println("==================================================")
            
            captureLogcat()
            
            // 重新抛出异常以标记测试失败
            throw e
        }
    }
    
    private fun captureLogcat() {
        try {
            // 获取最近的 logcat 输出，重点关注错误和崩溃
            val process = Runtime.getRuntime().exec(arrayOf(
                "logcat",
                "-d",  // dump and exit
                "-t", "200",  // last 200 lines
                "*:W"  // Warning level and above
            ))
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                println(line)
                
                // 高亮显示关键错误
                if (line?.contains("FATAL EXCEPTION") == true ||
                    line?.contains("AndroidRuntime") == true ||
                    line?.contains("Process: net.xzos.upgradeall") == true ||
                    line?.contains("Native crash") == true ||
                    line?.contains("java.lang.") == true) {
                    println(">>> CRITICAL: $line")
                }
            }
            
            reader.close()
            
            // 也获取特定于应用的日志
            println("\n==================================================")
            println("APP-SPECIFIC LOGS:")
            println("==================================================")
            
            val appProcess = Runtime.getRuntime().exec(arrayOf(
                "logcat",
                "-d",
                "-t", "100",
                "--pid=${android.os.Process.myPid()}"
            ))
            
            val appReader = BufferedReader(InputStreamReader(appProcess.inputStream))
            while (appReader.readLine().also { line = it } != null) {
                println(line)
            }
            appReader.close()
            
        } catch (e: Exception) {
            println("Failed to capture logcat: ${e.message}")
            e.printStackTrace()
        }
    }
    
    @Test
    fun checkAppDependencies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        println("\n==================================================")
        println("CHECKING APP DEPENDENCIES")
        println("==================================================")
        
        // 检查关键权限
        val permissions = arrayOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
        )
        
        println("\nPermissions:")
        for (permission in permissions) {
            val hasPermission = context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            println("  $permission: ${if (hasPermission) "✅ GRANTED" else "❌ DENIED"}")
        }
        
        // 检查应用组件
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_ACTIVITIES or
                android.content.pm.PackageManager.GET_SERVICES or
                android.content.pm.PackageManager.GET_RECEIVERS
            )
            
            println("\nRegistered Activities: ${packageInfo.activities?.size ?: 0}")
            packageInfo.activities?.take(5)?.forEach { activity ->
                println("  - ${activity.name}")
            }
            
            println("\nRegistered Services: ${packageInfo.services?.size ?: 0}")
            packageInfo.services?.take(5)?.forEach { service ->
                println("  - ${service.name}")
            }
            
        } catch (e: Exception) {
            println("Failed to get package info: ${e.message}")
        }
        
        // 检查关键类是否可以加载
        println("\n==================================================")
        println("CLASS LOADING TEST")
        println("==================================================")
        
        val criticalClasses = listOf(
            "net.xzos.upgradeall.ui.home.MainActivity",
            "net.xzos.upgradeall.application.MyApplication",
            "net.xzos.upgradeall.core.manager.AppManager",
            "net.xzos.upgradeall.getter.NativeLib"
        )
        
        for (className in criticalClasses) {
            try {
                Class.forName(className)
                println("✅ $className - Loaded successfully")
            } catch (e: Throwable) {
                println("❌ $className - Failed to load: ${e.message}")
            }
        }
        
        // 检查 native 库
        println("\n==================================================")
        println("NATIVE LIBRARIES")
        println("==================================================")
        
        try {
            val libDir = context.applicationInfo.nativeLibraryDir
            println("Native library directory: $libDir")
            
            val libDirFile = java.io.File(libDir)
            if (libDirFile.exists()) {
                val libs = libDirFile.listFiles()
                if (libs != null && libs.isNotEmpty()) {
                    println("Found ${libs.size} native libraries:")
                    libs.forEach { lib ->
                        println("  - ${lib.name} (${lib.length()} bytes)")
                    }
                } else {
                    println("⚠️ No native libraries found in directory")
                }
            } else {
                println("⚠️ Native library directory does not exist")
            }
        } catch (e: Exception) {
            println("Failed to check native libraries: ${e.message}")
        }
    }
}