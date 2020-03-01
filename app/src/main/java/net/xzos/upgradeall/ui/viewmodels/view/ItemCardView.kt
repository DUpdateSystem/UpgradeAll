package net.xzos.upgradeall.ui.viewmodels.view

import net.xzos.dupdatesystem.core.server_manager.module.BaseApp


class ItemCardView internal constructor(
        val name: String? = null,
        val desc: String? = null,
        val extraData: ItemCardViewExtraData = ItemCardViewExtraData()
)

data class ItemCardViewExtraData(
        val app: BaseApp? = null,
        val uuid: String? = null
)
