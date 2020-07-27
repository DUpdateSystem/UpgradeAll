package net.xzos.upgradeall.ui.viewmodels.view

class CloudConfigListItemView(
        val name: String,
        val type: Int?,
        val hubName: String?,
        val uuid: String?
) {
    companion object {
        fun newEmptyInstance() =
                CloudConfigListItemView("", null, null, null)
    }
}
