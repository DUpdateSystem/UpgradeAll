package net.xzos.upgradeAll.ui.viewmodels.view

import net.xzos.upgradeAll.json.nongson.ItemCardViewExtraData

class ItemCardView internal constructor(val name: String?, val desc: String?, val api: String?, val extraData: ItemCardViewExtraData = ItemCardViewExtraData()) {
}