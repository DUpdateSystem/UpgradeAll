package net.xzos.upgradeall

import android.content.pm.PackageInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidPackageInstallerContractTest {
    @Test
    fun requestCarriesOnlyGetterTargetAndArtifactPath() {
        val request = AndroidPackageInstallerContract.parseRequest(
            mapOf(
                "package_name" to "com.example.app",
                "apk_path" to "/getter/downloads/example.apk",
            ),
        )

        assertEquals("com.example.app", request.packageName)
        assertEquals("/getter/downloads/example.apk", request.apkPath)
    }

    @Test
    fun requestRejectsMissingOrUnknownFields() {
        assertThrows(IllegalArgumentException::class.java) {
            AndroidPackageInstallerContract.parseRequest(
                mapOf("apk_path" to "/getter/downloads/example.apk"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            AndroidPackageInstallerContract.parseRequest(
                mapOf(
                    "package_name" to "com.example.app",
                    "apk_path" to "/getter/downloads/example.apk",
                    "infer_from_file" to true,
                ),
            )
        }
    }

    @Test
    fun callbackStatusesRemainPlatformTruth() {
        assertEquals(
            "pending_user_action",
            AndroidPackageInstallerContract.statusName(
                PackageInstaller.STATUS_PENDING_USER_ACTION,
            ),
        )
        assertEquals(
            "succeeded",
            AndroidPackageInstallerContract.statusName(PackageInstaller.STATUS_SUCCESS),
        )
        assertEquals(
            "aborted",
            AndroidPackageInstallerContract.statusName(
                PackageInstaller.STATUS_FAILURE_ABORTED,
            ),
        )
        assertEquals(
            "failed",
            AndroidPackageInstallerContract.statusName(
                PackageInstaller.STATUS_FAILURE_CONFLICT,
            ),
        )
    }
}
