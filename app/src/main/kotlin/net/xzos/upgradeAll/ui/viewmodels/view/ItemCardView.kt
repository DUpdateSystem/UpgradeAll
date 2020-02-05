package net.xzos.upgradeAll.ui.viewmodels.view

import net.xzos.upgradeAll.server.app.manager.module.App


class ItemCardView internal constructor(
        val name: String? = null,
        val desc: String? = null,
        val extraData: ItemCardViewExtraData = ItemCardViewExtraData()
)

data class ItemCardViewExtraData(
        val app: App? = null,
        val uuid: String? = null
)
