package net.xzos.upgradeall.ui.hubmanager

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.ui.base.list.ListItemTextView

class HubManagerListItemView(
        override val name: String,
        val uuid: String
) : ListItemTextView {
    val observable = HubEnableObservable(HubManager.getHub(uuid) != null, fun(enable) {
        switchHubExistStatus(uuid, enable)
    })

    private fun switchHubExistStatus(uuid: String, enable: Boolean) {
        runBlocking {
            if (enable)
                CloudConfigGetter.downloadCloudHubConfig(uuid) {}
            else HubManager.getHub(uuid)?.run {
                HubManager.removeHub(this@run)
            }
        }
    }
}