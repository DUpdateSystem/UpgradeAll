package net.xzos.upgradeall.getter.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledInventoryCollectorTest {
    @Test
    fun defaultOptionsFilterSelfAndSystemPackagesAndSortResults() {
        val result = InstalledInventoryCollector.collect(
            selfPackageName = "net.xzos.upgradeall",
            packages = listOf(
                rawPackage("org.fdroid.fdroid", label = "F-Droid"),
                rawPackage("android", label = "Android System", isSystem = true),
                rawPackage("net.xzos.upgradeall", label = "UpgradeAll"),
                rawPackage("com.termux", label = "Termux"),
            ),
            options = InstalledInventoryScanOptions(),
        )

        assertEquals(4, result.stats.totalSeen)
        assertEquals(2, result.stats.returned)
        assertEquals(1, result.stats.filteredSystem)
        assertEquals(1, result.stats.filteredSelf)
        assertEquals(listOf("com.termux", "org.fdroid.fdroid"), result.inventory.items.map { it.packageName })
        assertFalse(result.inventory.items.any { it.packageName.startsWith("android/") })
    }

    @Test
    fun optionsCanIncludeSelfAndSystemPackages() {
        val result = InstalledInventoryCollector.collect(
            selfPackageName = "net.xzos.upgradeall",
            packages = listOf(
                rawPackage("android", isSystem = true),
                rawPackage("net.xzos.upgradeall"),
            ),
            options = InstalledInventoryScanOptions(
                includeSystemApps = true,
                includeSelf = true,
            ),
        )

        assertEquals(2, result.stats.totalSeen)
        assertEquals(2, result.stats.returned)
        assertEquals(0, result.stats.filteredSystem)
        assertEquals(0, result.stats.filteredSelf)
        assertEquals(listOf("android", "net.xzos.upgradeall"), result.inventory.items.map { it.packageName })
    }

    @Test
    fun duplicatePackageNamesKeepTheLastFactDeterministically() {
        val result = InstalledInventoryCollector.collect(
            selfPackageName = "net.xzos.upgradeall",
            packages = listOf(
                rawPackage("org.fdroid.fdroid", label = "Old Label", versionCode = 1),
                rawPackage("org.fdroid.fdroid", label = "New Label", versionCode = 2),
            ),
            options = InstalledInventoryScanOptions(),
        )

        assertEquals(2, result.stats.totalSeen)
        assertEquals(1, result.stats.returned)
        assertEquals("New Label", result.inventory.items.single().label)
        assertEquals(2L, result.inventory.items.single().versionCode)
    }

    @Test
    fun blankPackageNamesAreSkippedWithoutCreatingPackageIds() {
        val result = InstalledInventoryCollector.collect(
            selfPackageName = "net.xzos.upgradeall",
            packages = listOf(
                rawPackage(" ", label = "Blank"),
                rawPackage("com.example.valid", label = "Valid"),
            ),
            options = InstalledInventoryScanOptions(),
        )

        assertEquals(2, result.stats.totalSeen)
        assertEquals(1, result.stats.returned)
        assertEquals("com.example.valid", result.inventory.items.single().packageName)
    }

    @Test
    fun inventoryContractMatchesGetterInstalledInventoryFormat() {
        val result = InstalledInventoryCollector.collect(
            selfPackageName = "net.xzos.upgradeall",
            packages = listOf(rawPackage("org.fdroid.fdroid")),
            options = InstalledInventoryScanOptions(),
        )

        assertEquals("upgradeall-installed-inventory", result.inventory.format)
        assertEquals(1, result.inventory.version)
        assertTrue(result.diagnostics.isEmpty())
    }

    private fun rawPackage(
        packageName: String,
        label: String? = null,
        versionName: String? = null,
        versionCode: Long? = null,
        isSystem: Boolean = false,
    ) = RawInstalledPackage(
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = versionCode,
        isSystem = isSystem,
    )
}
