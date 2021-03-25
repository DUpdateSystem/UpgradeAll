package net.xzos.upgradeall.ui.applist.base.normal

import android.app.Application
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.ui.applist.base.BaseAppListViewModel

class NormalAppHubListViewModel(_application: Application) : BaseAppListViewModel(_application) {
    private lateinit var item: NormalAppListItemView

    fun initData(item: NormalAppListItemView, app: App) {
        this.item = item
        initData(app)
        super.initData(item, app)
    }

    override fun initObserve(owner: LifecycleOwner) {
        super.initObserve(owner)
        versionList.observe(owner, {
            viewModelScope.launch(Dispatchers.Main) { renewStatusIcon() }
        })
    }

    private suspend fun getStatusIcon(): Int {
        return when (app.getReleaseStatusWaitRenew()) {
            Updater.APP_LATEST -> R.drawable.ic_check_mark_circle
            Updater.APP_OUTDATED -> R.drawable.ic_check_needupdate
            Updater.NETWORK_ERROR -> R.drawable.ic_del_or_error
            Updater.APP_NO_LOCAL -> R.drawable.ic_local_error
            else -> R.drawable.ic_check_mark_circle
        }
    }

    private suspend fun renewStatusIcon() {
        item.ivStatusVisibility.set(View.GONE)
        item.pbStatusVisibility.set(View.VISIBLE)
        withContext(Dispatchers.Default) {
            item.statusIcon.set(getStatusIcon())
        }
        item.ivStatusVisibility.set(View.VISIBLE)
        item.pbStatusVisibility.set(View.GONE)
    }
}