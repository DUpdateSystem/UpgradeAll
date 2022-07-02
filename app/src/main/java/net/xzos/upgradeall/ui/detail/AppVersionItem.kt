package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.annotation.ColorRes
import androidx.databinding.ObservableField
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.wrapper.core.isIgnored

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
        app.versionList.firstOrNull { it.isIgnored(app) }?.run {
            markVersionNumberVisibility.set(true)
            markVersionNumber.set(
                getVersionNameSpannableStringWithRes(
                    versionInfo.versionCharList, null, null, context
                )
            )
        } ?: kotlin.run {
            markVersionNumberVisibility.set(false)
        }
    }

    companion object {
        private fun getShowingVersionNumber(
            app: App, context: Context,
            @ColorRes normalColorRes: Int?, @ColorRes lowLevelColorRes: Int?,
        ): SpannableStringBuilder {
            val sb = SpannableStringBuilder()
            val latestVersionNumber = runBlocking { app.getLatestVersionNumber() }
            val localVersion = app.localVersion
            val installedVersionNumber = localVersion?.name
            localVersion?.versionCharList?.run {
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