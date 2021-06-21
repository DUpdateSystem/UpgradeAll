package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.Version

class AppVersionItem {
    val versionNumberVisibility: ObservableField<Boolean> = ObservableField()
    val showingVersionNumber: ObservableField<SpannableStringBuilder> = ObservableField()
    val markVersionNumberVisibility: ObservableField<Boolean> = ObservableField()
    val markVersionNumber: ObservableField<SpannableStringBuilder> = ObservableField()

    fun renew(showingVersion: SpannableStringBuilder?, app: App, context: Context) {
        if (showingVersion != null) {
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
                    markVersion!!.rawVersionStringList, R.color.colorPrimary, context
                )
            )
        } else {
            markVersionNumberVisibility.set(false)
        }
    }
}