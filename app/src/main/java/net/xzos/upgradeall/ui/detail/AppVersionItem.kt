package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.annotation.ColorRes
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionUtils

class AppVersionItem {
    val versionNumberVisibility: ObservableField<Boolean> = ObservableField()
    val showingVersionNumber: ObservableField<SpannableStringBuilder> = ObservableField()
    val markVersionNumberVisibility: ObservableField<Boolean> = ObservableField()
    val markVersionNumber: ObservableField<SpannableStringBuilder> = ObservableField()

    @ColorRes
    var normalColorRes: Int? = null

    @ColorRes
    var lowLevelColorRes: Int? = R.color.text_low_priority_color

    fun renew(app: App, context: Context) {
        val showingVersion = getShowingVersionNumber(
            app, context,
            normalColorRes, lowLevelColorRes
        )
        if (showingVersion.isNotBlank()) {
            versionNumberVisibility.set(true)
            showingVersionNumber.set(showingVersion)
        } else {
            versionNumberVisibility.set(false)
        }
        var markVersion: Version? = null
        app.versionList.forEach {
            if (it.isIgnored)
                markVersion = it
        }
        if (markVersion != null) {
            markVersionNumberVisibility.set(true)
            markVersionNumber.set(
                getVersionNameSpannableStringWithRes(
                    markVersion!!.rawVersionStringList, null, null, context
                )
            )
        } else {
            markVersionNumberVisibility.set(false)
        }
    }

    companion object {
        private fun getShowingVersionNumber(
            app: App, context: Context,
            @ColorRes normalColorRes: Int?, @ColorRes lowLevelColorRes: Int?,
        ): SpannableStringBuilder {
            val sb = SpannableStringBuilder()
            val latestVersionNumber = app.getLatestVersionNumber()
            val rawInstalledVersionStringList = app.rawInstalledVersionStringList
            val installedVersionNumber = if (rawInstalledVersionStringList != null)
                VersionUtils.getKey(rawInstalledVersionStringList)
            else null
            rawInstalledVersionStringList?.run {
                getVersionNameSpannableStringWithRes(
                    this, normalColorRes, lowLevelColorRes,
                    context, sb
                )
            }
            if (latestVersionNumber != installedVersionNumber && latestVersionNumber != null) {
                if (sb.isNotEmpty()) sb.append(" -> ")
                sb.append(latestVersionNumber)
            }
            return sb
        }
    }
}