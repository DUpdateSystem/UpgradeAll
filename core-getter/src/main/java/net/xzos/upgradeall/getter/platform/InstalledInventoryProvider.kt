package net.xzos.upgradeall.getter.platform

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

private const val INSTALLED_INVENTORY_FORMAT = "upgradeall-installed-inventory"
private const val INSTALLED_INVENTORY_VERSION = 1

/**
 * JNI entrypoint used by Rust's platform adapter.
 *
 * Kotlin returns raw Android PackageManager facts only. It does not construct
 * UpgradeAll package ids, decide repository coverage, generate Lua, or write
 * getter storage.
 */
@Suppress("unused")
object InstalledInventoryProvider {
    @JvmStatic
    fun scanInstalledInventory(context: Context, optionsJson: String): String {
        val options = InstalledInventoryJson.decodeOptions(optionsJson)
        val result = InstalledInventoryScanner.scan(context.applicationContext ?: context, options)
        return InstalledInventoryJson.encodeResult(result)
    }
}

object InstalledInventoryScanner {
    fun scan(
        context: Context,
        options: InstalledInventoryScanOptions = InstalledInventoryScanOptions(),
    ): InstalledInventoryScanResult {
        val packageManager = context.packageManager
        val rawPackages = getInstalledPackages(packageManager).map { packageInfo ->
            packageInfo.toRawInstalledPackage(packageManager)
        }
        val result = InstalledInventoryCollector.collect(
            selfPackageName = context.packageName,
            packages = rawPackages,
            options = options,
        )
        val diagnostics = result.diagnostics.toMutableList()
        if (!declaresQueryAllPackages(context)) {
            diagnostics += PlatformDiagnostic(
                code = "package_visibility.query_all_packages_missing",
                message = "QUERY_ALL_PACKAGES is not declared; installed app inventory may be incomplete.",
            )
        }
        return result.copy(diagnostics = diagnostics)
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackages(packageManager: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getInstalledPackages(0)
        }
    }

    private fun PackageInfo.toRawInstalledPackage(packageManager: PackageManager): RawInstalledPackage {
        val appInfo = applicationInfo
        return RawInstalledPackage(
            packageName = packageName.orEmpty(),
            label = appInfo.safeLabel(packageManager),
            versionName = versionName?.takeIf { it.isNotBlank() },
            versionCode = packageVersionCode(),
            isSystem = appInfo.isSystemPackage(),
        )
    }

    private fun ApplicationInfo?.safeLabel(packageManager: PackageManager): String? {
        return try {
            this?.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun ApplicationInfo?.isSystemPackage(): Boolean {
        val flags = this?.flags ?: return false
        return flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.packageVersionCode(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            versionCode.toLong()
        }
    }

    private fun declaresQueryAllPackages(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            }
            packageInfo.requestedPermissions?.contains(Manifest.permission.QUERY_ALL_PACKAGES) == true
        } catch (_: RuntimeException) {
            false
        }
    }
}

object InstalledInventoryCollector {
    fun collect(
        selfPackageName: String,
        packages: List<RawInstalledPackage>,
        options: InstalledInventoryScanOptions,
    ): InstalledInventoryScanResult {
        var filteredSystem = 0
        var filteredSelf = 0
        val itemsByPackageName = linkedMapOf<String, InstalledInventoryItem>()

        for (rawPackage in packages) {
            val packageName = rawPackage.packageName.trim()
            if (packageName.isEmpty()) {
                continue
            }
            if (!options.includeSelf && packageName == selfPackageName) {
                filteredSelf++
                continue
            }
            if (!options.includeSystemApps && rawPackage.isSystem) {
                filteredSystem++
                continue
            }
            itemsByPackageName[packageName] = InstalledInventoryItem(
                packageName = packageName,
                label = rawPackage.label?.takeIf { it.isNotBlank() },
                versionName = rawPackage.versionName?.takeIf { it.isNotBlank() },
                versionCode = rawPackage.versionCode,
            )
        }

        val items = itemsByPackageName.values.sortedBy { it.packageName }
        return InstalledInventoryScanResult(
            inventory = InstalledInventory(items = items),
            stats = InstalledInventoryScanStats(
                totalSeen = packages.size,
                returned = items.size,
                filteredSystem = filteredSystem,
                filteredSelf = filteredSelf,
            ),
        )
    }
}

data class InstalledInventoryScanOptions(
    val includeSystemApps: Boolean = false,
    val includeSelf: Boolean = false,
)

data class RawInstalledPackage(
    val packageName: String,
    val label: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val isSystem: Boolean = false,
)

data class InstalledInventoryScanResult(
    val inventory: InstalledInventory,
    val stats: InstalledInventoryScanStats,
    val diagnostics: List<PlatformDiagnostic> = emptyList(),
)

data class InstalledInventory(
    val format: String = INSTALLED_INVENTORY_FORMAT,
    val version: Int = INSTALLED_INVENTORY_VERSION,
    val items: List<InstalledInventoryItem> = emptyList(),
)

data class InstalledInventoryItem(
    val packageName: String,
    val label: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
)

data class InstalledInventoryScanStats(
    val totalSeen: Int,
    val returned: Int,
    val filteredSystem: Int,
    val filteredSelf: Int,
)

data class PlatformDiagnostic(
    val code: String,
    val message: String,
    val detail: String? = null,
)

object InstalledInventoryJson {
    fun decodeOptions(json: String): InstalledInventoryScanOptions {
        val value = if (json.isBlank()) JSONObject() else JSONObject(json)
        return InstalledInventoryScanOptions(
            includeSystemApps = value.optBoolean("include_system_apps", false),
            includeSelf = value.optBoolean("include_self", false),
        )
    }

    fun encodeResult(result: InstalledInventoryScanResult): String {
        return JSONObject()
            .put("inventory", encodeInventory(result.inventory))
            .put("stats", encodeStats(result.stats))
            .put("diagnostics", JSONArray().also { diagnostics ->
                result.diagnostics.forEach { diagnostics.put(encodeDiagnostic(it)) }
            })
            .toString()
    }

    private fun encodeInventory(inventory: InstalledInventory): JSONObject {
        return JSONObject()
            .put("format", inventory.format)
            .put("version", inventory.version)
            .put("items", JSONArray().also { items ->
                inventory.items.forEach { items.put(encodeItem(it)) }
            })
    }

    private fun encodeItem(item: InstalledInventoryItem): JSONObject {
        return JSONObject()
            .put("kind", "android_package")
            .put("package_name", item.packageName)
            .putNullable("label", item.label)
            .putNullable("version_name", item.versionName)
            .putNullable("version_code", item.versionCode)
    }

    private fun encodeStats(stats: InstalledInventoryScanStats): JSONObject {
        return JSONObject()
            .put("total_seen", stats.totalSeen)
            .put("returned", stats.returned)
            .put("filtered_system", stats.filteredSystem)
            .put("filtered_self", stats.filteredSelf)
    }

    private fun encodeDiagnostic(diagnostic: PlatformDiagnostic): JSONObject {
        return JSONObject()
            .put("code", diagnostic.code)
            .put("message", diagnostic.message)
            .putNullable("detail", diagnostic.detail)
    }

    private fun JSONObject.putNullable(name: String, value: Any?): JSONObject {
        return put(name, value ?: JSONObject.NULL)
    }
}
