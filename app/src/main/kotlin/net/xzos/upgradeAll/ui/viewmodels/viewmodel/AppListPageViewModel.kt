package net.xzos.upgradeAll.ui.viewmodels.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.json.nongson.ItemCardViewExtraData
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import org.litepal.LitePal
import org.litepal.extension.find

class AppListPageViewModel : ViewModel() {

    private val mHubUuid = MutableLiveData<String>()
    internal val appCardViewList: LiveData<MutableList<ItemCardView>> = Transformations.map(mHubUuid) { hubUuid ->
        return@map mutableListOf<ItemCardView>().apply {
            val repoDatabases: List<RepoDatabase> = LitePal.where("api_uuid = ?", hubUuid).find()
            for (repoDatabase in repoDatabases) {
                this.add(getAppItemCardView(repoDatabase))
            }
            if (this.isNotEmpty()) {
                this.add(ItemCardView(Pair(null, null), null, null, ItemCardViewExtraData(isEmpty = true)))
            }
        }
    }

    internal val needUpdateAppIdLiveLiveData = MutableLiveData(mutableListOf<Long>())

    internal fun setHubUuid(hubUuid: String) {
        mHubUuid.value = hubUuid
    }

    private fun getAppItemCardView(item: RepoDatabase): ItemCardView {
        val iconInfo: Pair<String?, String?> = Pair(
                runBlocking { ServerContainer.AppManager.getApp(item.id).engine.getAppIconUrl() }
                , item.versionCheckerGson?.text
        )
        val databaseId = item.id
        val name = item.name
        val url = item.url
        return ItemCardView(iconInfo, name, url, ItemCardViewExtraData(databaseId = databaseId))
    }
}