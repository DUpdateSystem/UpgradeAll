package net.xzos.upgradeall.getter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class GetterPortGithubUnitTest {
    private val nativeLib = NativeLib()
    private val hubUuid = "fd9b2602-62c5-4d55-bd1e-0d6537714ca0"
    private val idMap = mapOf(
        "owner" to "DUpdateSystem",
        "repo" to "UpgradeAll",
    )

    @Test
    fun check_app_available() {
        assert(nativeLib.checkAppAvailable(hubUuid, idMap))
    }

    @Test
    fun get_app_latest_release() {
        assert(nativeLib.getAppLatestRelease(hubUuid, idMap).isNotEmpty())
    }

    @Test
    fun get_app_releases() {
        assert(nativeLib.getAppReleases(hubUuid, idMap).isNotEmpty())
    }
}