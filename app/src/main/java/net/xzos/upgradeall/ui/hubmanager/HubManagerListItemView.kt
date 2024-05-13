package net.xzos.upgradeall.ui.hubmanager

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.ui.base.databinding.EnableObservable
import net.xzos.upgradeall.ui.base.list.ActivityListItemView
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.websdk.data.json.HubConfigGson

class HubManagerListItemView(
    name: String, val uuid: String
) : BaseAppIconItem, ActivityListItemView {
    private val hub get() = HubManager.getHub(uuid)
    val enableObservable = EnableObservable(hub != null) { enable ->
        switchHubExistStatus(uuid, enable)
    }

    val applicationsAvailableObservable = ObservableBoolean(
        enableObservable.enable && hub?.applicationsModeAvailable() == true
    )

    val applicationsEnableObservable =
        EnableObservable(hub?.isEnableApplicationsMode() == true) { enable ->
            runBlocking(Dispatchers.Default) {
                hub?.setApplicationsMode(enable)
            }
        }

    private fun switchHubExistStatus(uuid: String, enable: Boolean) {
        runBlocking {
            if (enable)
                CloudConfigGetter.downloadCloudHubConfig(uuid) {}
            else hub?.run {
                HubManager.removeHub(this@run)
            }
        }
        applicationsAvailableObservable.set(
            enableObservable.enable && hub?.applicationsModeAvailable() == true
        )
    }

    override val appName: ObservableField<String> = ObservableField(name)
    override val nameFirst: ObservableField<String> = ObservableField()
    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override fun getItemIdName(): String {
        return appName.get().toString()
    }

    companion object {
        fun getCloudHubItemCardView(
            hubConfig: HubConfigGson,
            context: Context
        ): HubManagerListItemView {
            val name = hubConfig.info.hubName
            val uuid = hubConfig.uuid
            return HubManagerListItemView(name, uuid).apply {
                renewAppIcon(null, context)
            }
        }
    }
}