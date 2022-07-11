package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpanned
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.VersionWrapper
import net.xzos.upgradeall.ui.base.list.BaseAppIconItem
import net.xzos.upgradeall.ui.detail.download.DownloadStatusData
import net.xzos.upgradeall.utils.MiscellaneousUtils.hasHTMLTags

class AppDetailItem : BaseAppIconItem {
    override val appName: ObservableField<String> = ObservableField()
    val appPackageId: ObservableField<CharSequence> = ObservableField()
    val urlLayoutVisibility: ObservableField<Boolean> = ObservableField()
    val showingURL: ObservableField<String> = ObservableField()
    val ivMoreURLVisibility: ObservableField<Boolean> = ObservableField()
    var appUrlList: Set<String> = setOf()

    val versionItem = AppVersionItem()

    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override val nameFirst: ObservableField<String> = ObservableField()

    fun renewVersionItem(app: App, context: Context) {
        versionItem.renew(app, context)
    }

    fun setAppUrl(app: App) {
        val urlList = app.hubList.mapNotNull {
            app.getUrl(it.uuid)
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

    fun setAssetInfo(versionList: List<VersionWrapper>?, context: Context) {
        versionList ?: return
        val latestChangeLog = SpannableStringBuilder()
        versionList.forEachIndexed { index, version ->
            val changelog = version.release.changelog
            if (!changelog.isNullOrBlank()) {
                val colorSpan = ForegroundColorSpan(Color.BLUE)
                val start = latestChangeLog.length
                val hubName = version.hub.name
                latestChangeLog.append("$hubName\n")
                latestChangeLog.setSpan(
                    colorSpan, start, start + hubName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                latestChangeLog.append(getChangelogSpanned(changelog, context))
                if (index < versionList.size - 1) latestChangeLog.append("\n")
            }
        }
        val length = latestChangeLog.length
        val spannableStringBuilder = if (length != 0)
            latestChangeLog.delete(length - 1, length)
        else latestChangeLog
        changelog.set(spannableStringBuilder)
    }

    private fun getChangelogSpanned(changelog: String?, context: Context): Spanned {
        return when {
            changelog.isNullOrBlank() -> context.getString(R.string.null_english).toSpanned()
            changelog.hasHTMLTags() -> {
                HtmlCompat.fromHtml(changelog, HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS)
            }
            else -> changelog.toSpanned()
        }
    }

    val changelog: ObservableField<CharSequence> = ObservableField()
    val downloadData = DownloadStatusData()
}