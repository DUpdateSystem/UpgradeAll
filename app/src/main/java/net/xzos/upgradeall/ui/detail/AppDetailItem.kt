package net.xzos.upgradeall.ui.detail

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.text.HtmlCompat
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.Version
import net.xzos.upgradeall.ui.base.list.ListItemTextView
import net.xzos.upgradeall.ui.detail.download.DownloadStatusData
import net.xzos.upgradeall.utils.MiscellaneousUtils.hasHTMLTags
import kotlin.properties.Delegates

class AppDetailItem(private val activity: AppDetailActivity) : ListItemTextView {
    override val appName: ObservableField<String> = ObservableField()
    val appPackageId: ObservableField<CharSequence> = ObservableField()
    val showingVersionNumber: ObservableField<String> = ObservableField()

    override val appIcon: ObservableField<Drawable> = ObservableField()
    override val iconBackgroundTint: ObservableField<ColorStateList?> = ObservableField()

    override val nameFirst: ObservableField<String> = ObservableField()

    var selectedVersion: Version? by Delegates.observable(null) { _, _, new ->
        if (new == null) return@observable
        var latestChangeLog = ""
        for (asset in new.assetList) {
            val changelog = asset.changelog
            if (!changelog.isNullOrBlank()) {
                latestChangeLog += "${asset.hub.name}\n${changelog}\n"
            }
        }
        setChangelog(latestChangeLog)
    }

    private fun setChangelog(_changelog: CharSequence?) {
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