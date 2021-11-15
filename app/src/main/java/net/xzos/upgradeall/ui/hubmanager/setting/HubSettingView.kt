package net.xzos.upgradeall.ui.hubmanager.setting

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField

class HubSettingView(
    useGlobalSetting: Boolean?,
    match: String?,
    replace: String?
) {
    val useGlobalSetting = ObservableBoolean(useGlobalSetting ?: false)
    val useGlobalSettingVisibility = useGlobalSetting != null
    val matchRule = ObservableField(match)
    val replaceString = ObservableField(replace)
}