import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import java.io.File
import com.android.build.gradle.LibraryExtension

class RustJNIPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("rustJNI", RustJNIExtension::class.java)
        
        project.afterEvaluate {
            val rustJNI = project.extensions.getByType(RustJNIExtension::class.java)
            
            // Register the cargo build task
            val cargoBuildTask = project.tasks.register<CargoBuildTask>("cargoBuild") {
                module.set(rustJNI.module)
                targets.set(rustJNI.targets)
                profile.set(rustJNI.profile)
                libname.set(rustJNI.libname)
            }
            
            // Configure Android library to include JNI libraries
            project.extensions.findByType<LibraryExtension>()?.let { android ->
                android.sourceSets.getByName("main").jniLibs.srcDirs(
                    File(project.layout.buildDirectory.asFile.get(), "rustJniLibs/android")
                )
            }
            
            // Make the preBuild task depend on cargo build
            project.tasks.named("preBuild") {
                dependsOn(cargoBuildTask)
            }
        }
    }
}

open class RustJNIExtension {
    var module: String = ""
    var targets: List<String> = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
    var profile: String = "release"
    var libname: String = ""
}

abstract class CargoBuildTask : DefaultTask() {
    @get:Input
    abstract val module: org.gradle.api.provider.Property<String>
    
    @get:Input
    abstract val targets: org.gradle.api.provider.ListProperty<String>
    
    @get:Input
    abstract val profile: org.gradle.api.provider.Property<String>
    
    @get:Input
    abstract val libname: org.gradle.api.provider.Property<String>
    
    @TaskAction
    fun build() {
        val moduleDir = File(project.projectDir, module.get())
        val outputDir = File(project.layout.buildDirectory.asFile.get(), "rustJniLibs/android")
        
        if (!moduleDir.exists()) {
            throw IllegalStateException("Rust module directory does not exist: $moduleDir")
        }
        
        // Map Android ABI to Rust target triples
        val targetMap = mapOf(
            "arm64-v8a" to "aarch64-linux-android",
            "armeabi-v7a" to "armv7-linux-androideabi",
            "x86_64" to "x86_64-linux-android",
            "x86" to "i686-linux-android"
        )
        
        // Set up environment variables for Android NDK
        val ndkVersion = "26.3.11579264"
        
        // Try to get Android SDK path from multiple sources
        val androidHome = System.getenv("ANDROID_HOME") 
            ?: project.findProperty("sdk.dir")?.toString()
            ?: run {
                val localProperties = File(project.rootDir, "local.properties")
                if (localProperties.exists()) {
                    val props = java.util.Properties()
                    localProperties.inputStream().use { props.load(it) }
                    props.getProperty("sdk.dir")
                } else null
            }
            ?: throw IllegalStateException("ANDROID_HOME not set and sdk.dir not found in local.properties")
        
        val ndkPath = File(androidHome, "ndk/$ndkVersion")
        
        if (!ndkPath.exists()) {
            throw IllegalStateException("NDK not found at: $ndkPath")
        }
        
        val hostOS = when {
            System.getProperty("os.name").lowercase().contains("linux") -> "linux-x86_64"
            System.getProperty("os.name").lowercase().contains("mac") -> "darwin-x86_64"
            System.getProperty("os.name").lowercase().contains("win") -> "windows-x86_64"
            else -> throw IllegalStateException("Unsupported host OS")
        }
        
        // Build for each target
        targets.get().forEach { abi ->
            val rustTarget = targetMap[abi] ?: throw IllegalStateException("Unknown ABI: $abi")
            val abiOutputDir = File(outputDir, abi)
            abiOutputDir.mkdirs()
            
            println("Building Rust library for $rustTarget...")
            
            // Set up cargo config for cross-compilation
            val cargoConfigDir = File(moduleDir, ".cargo")
            cargoConfigDir.mkdirs()
            val cargoConfig = File(cargoConfigDir, "config.toml")
            
            val apiLevel = when (rustTarget) {
                "armv7-linux-androideabi" -> "21"
                else -> "21"
            }
            
            val clangTarget = when (rustTarget) {
                "armv7-linux-androideabi" -> "armv7a-linux-androideabi"
                else -> rustTarget
            }
            
            cargoConfig.writeText("""
                [target.$rustTarget]
                ar = "${ndkPath}/toolchains/llvm/prebuilt/$hostOS/bin/llvm-ar"
                linker = "${ndkPath}/toolchains/llvm/prebuilt/$hostOS/bin/${clangTarget}${apiLevel}-clang"
                
                [env]
                CC_${rustTarget.replace("-", "_")} = "${ndkPath}/toolchains/llvm/prebuilt/$hostOS/bin/${clangTarget}${apiLevel}-clang"
                AR_${rustTarget.replace("-", "_")} = "${ndkPath}/toolchains/llvm/prebuilt/$hostOS/bin/llvm-ar"
            """.trimIndent())
            
            // Run cargo build
            val profileFlag = if (profile.get() == "release") "--release" else ""
            val targetFlag = "--target=$rustTarget"
            
            val commandList = mutableListOf("cargo", "build")
            if (profileFlag.isNotEmpty()) commandList.add(profileFlag)
            commandList.add(targetFlag)
            
            val process = ProcessBuilder(commandList).apply {
                directory(moduleDir)
                environment()["ANDROID_NDK_HOME"] = ndkPath.absolutePath
                environment()["CC"] = "${ndkPath}/toolchains/llvm/prebuilt/$hostOS/bin/${clangTarget}${apiLevel}-clang"
                environment()["AR"] = "${ndkPath}/toolchains/llvm/prebuilt/$hostOS/bin/llvm-ar"
                redirectErrorStream(true)
            }.start()
            
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            
            if (exitCode != 0) {
                println(output)
                throw RuntimeException("Cargo build failed for $rustTarget")
            }
            
            println("Build successful for $rustTarget")
            
            // Copy the built library to the output directory
            val profileDir = if (profile.get() == "release") "release" else "debug"
            val sourceLib = File(moduleDir, "target/$rustTarget/$profileDir/lib${libname.get()}.so")
            val destLib = File(abiOutputDir, "lib${libname.get()}.so")
            
            if (sourceLib.exists()) {
                sourceLib.copyTo(destLib, overwrite = true)
                println("Copied library to: $destLib")
            } else {
                throw RuntimeException("Built library not found: $sourceLib")
            }
        }
    }
}