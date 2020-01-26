package net.xzos.upgradeAll.ui.viewmodels.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.data.json.nongson.ItemCardViewExtraData
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.manager.AppManager
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView

class AppListPageViewModel : ViewModel() {

    private val mHubUuid = MutableLiveData<String>()
    internal val appCardViewList: LiveData<MutableList<ItemCardView>> = Transformations.map(mHubUuid) { hubUuid ->
        return@map mutableListOf<ItemCardView>().apply {
            val repoDatabases = AppDatabaseManager.getDatabaseList(hubUuid = hubUuid)
            for (repoDatabase in repoDatabases) {
                repoDatabase?.let { this.add(getAppItemCardView(it)) }
            }
            if (this.isNotEmpty()) {
                this.add(ItemCardView(Pair(null, null), null, null, ItemCardViewExtraData(isEmpty = true)))
            }
        }
    }

    internal fun setHubUuid(hubUuid: String) {
        mHubUuid.value = hubUuid
    }

    private fun getAppItemCardView(item: RepoDatabase): ItemCardView {
        val iconInfo: Pair<String?, String?> = Pair(
                runBlocking { AppManager.getApp(item.id).engine.getAppIconUrl() }
                , item.targetChecker?.extraString
        )
        val databaseId = item.id
        val name = item.name
        val url = item.url
        return ItemCardView(iconInfo, name, url, ItemCardViewExtraData(databaseId = databaseId))
    }

    internal val needUpdateAppIdLiveLiveData = MutableLiveData(mutableListOf<Long>())
}