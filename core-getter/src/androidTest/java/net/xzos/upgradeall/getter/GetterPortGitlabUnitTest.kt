package net.xzos.upgradeall.getter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.io.path.createTempDirectory

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class GetterPortGitlabUnitTest {
    private val config = RustConfig(
        cacheDir = createTempDirectory().toFile(),
        dataDir = createTempDirectory().toFile(),
        globalExpireTime = 60,
    )
    private val getterPort = GetterPort(config)
    private val hubUuid = "a84e2fbe-1478-4db5-80ae-75d00454c7eb"
    private val appDataMap = mapOf(
        "owner" to "fdroid",
        "repo" to "fdroidclient",
    )
    private val hubDataMap = mapOf<String, String>()

    @Test
    fun check_init() {
        assert(getterPort.init())
    }

    @Test
    fun check_app_available() {
        assert(getterPort.checkAppAvailable(hubUuid, appDataMap, hubDataMap) == true)
    }

    @Test
    fun get_app_latest_release() {
        getterPort.getAppLatestRelease(hubUuid, appDataMap, hubDataMap)
    }

    @Test
    fun get_app_releases() {
        assert(!getterPort.getAppReleases(hubUuid, appDataMap, hubDataMap).isNullOrEmpty())
    }
}