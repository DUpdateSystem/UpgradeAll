package net.xzos.upgradeall.ui.hubmanager

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.json.HubConfigGson
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.ui.base.list.ActivityListItemView
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem

class HubManagerListItemView(
        name: String,
        private val uuid: String
) : BaseAppIconItem, ActivityListItemView {
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

    override val appName: ObservableField<String> = ObservableField(name)
    override val nameFirst: ObservableField<String> = ObservableField()
    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override fun getItemIdName(): String {
        return appName.get().toString()
    }

    companion object {
        fun getCloudHubItemCardView(hubConfig: HubConfigGson, context: Context): HubManagerListItemView {
            val name = hubConfig.info.hubName
            val uuid = hubConfig.uuid
            return HubManagerListItemView(name, uuid).apply {
                renewAppIcon(null, context)
            }
        }
    }
}