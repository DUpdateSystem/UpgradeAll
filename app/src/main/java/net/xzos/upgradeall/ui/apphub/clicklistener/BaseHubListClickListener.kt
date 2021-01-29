package net.xzos.upgradeall.ui.apphub.clicklistener

class BaseHubListClickListener<T>(val clickListener: (T) -> Unit) {
    fun onClick(item: T) = clickListener(item)
}