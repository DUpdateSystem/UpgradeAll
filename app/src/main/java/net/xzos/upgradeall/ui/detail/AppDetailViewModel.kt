package net.xzos.upgradeall.ui.detail

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.BaseObservable
import androidx.databinding.ObservableField
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.core.utils.getPackageId
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.MiscellaneousUtils.hasHTMLTags
import net.xzos.upgradeall.utils.UxUtils

class AppDetailViewModel(val app: App) : ListItemView, BaseObservable() {

    override val name get() = app.name
    val packageName get() = app.appId.getPackageId()?.second
    override val nameFirst: String
        get() {
            return if (appIcon == null)
                super.nameFirst
            else ""
        }

    val iconBackgroundTint get() = if (appIcon == null) ColorStateList.valueOf(UxUtils.getRandomColor()) else null

    private val appIcon: Drawable? by lazy {
        app.appId.getPackageId()?.second?.run {
            MiscellaneousUtils.getAppIcon(context, this)?.run {
                return@lazy this
            }
        }
        return@lazy null
    }
    val icon: Drawable by lazy {
        return@lazy appIcon ?: ContextCompat.getDrawable(context, R.drawable.bg_circle)!!
    }

    val version: String? get() = app.installedVersionNumber

    val versionList: List<Version> by lazy { runBlocking { app.versionList }.asReversed() }

    // 下载状态信息
    val downloadData = DownloadStatusData()

    var currentVersion: Version? = null
    val changelogData = ObservableField<CharSequence>()
    private fun setChangelog(changelog: CharSequence?) {
        changelogData.set(when {
            changelog.isNullOrBlank() -> {
                context.getString(R.string.null_english)
            }
            changelog.hasHTMLTags() -> {
                HtmlCompat.fromHtml(changelog.toString(), HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS)
            }
            else -> changelog
        })
    }

    fun setVersionInfo(position: Int) {
        if (position >= versionList.size) return
        val index = if (position < 0)
            versionList.size + position
        else position
        val versionItem = versionList[index]
        currentVersion = versionItem
        var latestChangeLog = ""
        for (asset in versionItem.assetList) {
            val changelog = asset.changeLog
            if (!changelog.isNullOrBlank()) {
                latestChangeLog += "${asset.hub.name}\n${changelog}\n"
            }
        }
        setChangelog(latestChangeLog)
    }

    override fun equals(other: Any?): Boolean {
        return other is AppDetailViewModel
                && other.app == app
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + app.hashCode()
        return result
    }

    companion object {
        private val context get() = MyApplication.context
    }
}