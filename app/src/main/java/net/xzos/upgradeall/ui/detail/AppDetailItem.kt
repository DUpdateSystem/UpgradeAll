package net.xzos.upgradeall.ui.detail

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.HtmlCompat
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Asset
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.ui.detail.download.DownloadStatusData
import net.xzos.upgradeall.utils.MiscellaneousUtils.hasHTMLTags

class AppDetailItem(private val activity: AppDetailActivity) : BaseAppIconItem {
    override val appName: ObservableField<String> = ObservableField()
    val appPackageId: ObservableField<CharSequence> = ObservableField()
    val showingVersionNumber: ObservableField<String> = ObservableField()
    val versionNumberVisibility: ObservableField<Boolean> = ObservableField()
    val urlLayoutVisibility: ObservableField<Boolean> = ObservableField()
    val showingURL: ObservableField<String> = ObservableField()
    val ivMoreURLVisibility: ObservableField<Boolean> = ObservableField()
    var appUrlList: Set<String> = setOf()

    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override val nameFirst: ObservableField<String> = ObservableField()

    fun setInstallViewNumber(installedVersionName: String) {
        showingVersionNumber.set(installedVersionName)
        versionNumberVisibility.set(installedVersionName.isNotBlank())
    }

    fun setAppUrl(app: App) {
        val urlList = app.hubListUuid.mapNotNull {
            app.getUrl(it)
        }.toSet()
        val mainUrl = urlList.firstOrNull()
        if (mainUrl == null) {
            urlLayoutVisibility.set(false)
        } else {
            urlLayoutVisibility.set(true)
            showingURL.set(mainUrl)
        }
        ivMoreURLVisibility.set(urlList.size > 1)
        appUrlList = urlList
    }

    fun setAssetInfo(assetList: List<Asset>?) {
        assetList ?: return
        val latestChangeLog = SpannableStringBuilder()
        assetList.forEach { asset ->
            val changelog = asset.changelog
            if (!changelog.isNullOrBlank()) {
                val colorSpan = ForegroundColorSpan(Color.BLUE)
                val start = latestChangeLog.length
                val hubName = asset.hub.name
                latestChangeLog.append("$hubName\n${changelog}\n\n")
                latestChangeLog.setSpan(colorSpan, start, start + hubName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                latestChangeLog.append()
            }
        }
        val length = latestChangeLog.length
        val spannableStringBuilder = if (length != 0)
            latestChangeLog.delete(length - 1, length)
        else latestChangeLog
        setChangelog(spannableStringBuilder)
    }

    private fun setChangelog(_changelog: SpannableStringBuilder?) {
        changelog.set(when {
            _changelog.isNullOrBlank() -> {
                activity.getString(R.string.null_english)
            }
            _changelog.hasHTMLTags() -> {
                HtmlCompat.fromHtml(_changelog.toString(), HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS)
            }
            else -> _changelog
        })
    }

    val changelog: ObservableField<CharSequence> = ObservableField()
    val downloadData = DownloadStatusData()
}