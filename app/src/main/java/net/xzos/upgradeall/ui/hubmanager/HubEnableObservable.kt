package net.xzos.upgradeall.ui.hubmanager

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import net.xzos.upgradeall.BR

class HubEnableObservable(enableHub: Boolean, private val changedFun: (enable:Boolean) -> Unit) : BaseObservable() {

    @get:Bindable
    var enableHub: Boolean = enableHub
        set(value) {
            field = value
            changedFun(value)
            notifyPropertyChanged(BR.enableHub)
        }
}