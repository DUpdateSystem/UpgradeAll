package net.xzos.upgradeall.ui.base.databinding

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import net.xzos.upgradeall.BR

class EnableObservable(enable: Boolean, private val changedFun: (enable: Boolean) -> Unit) :
    BaseObservable() {

    @get:Bindable
    var enable: Boolean = enable
        set(value) {
            field = value
            changedFun(value)
            notifyPropertyChanged(BR.enable)
        }
}