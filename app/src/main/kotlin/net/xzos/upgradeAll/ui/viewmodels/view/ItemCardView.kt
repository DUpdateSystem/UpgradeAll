package net.xzos.upgradeAll.ui.viewmodels.view

import net.xzos.upgradeAll.json.nongson.ItemCardViewExtraData

class ItemCardView internal constructor(
        // appIconInfo: Pair<Url, moduleName>
        // cloudHubIconInfo: Pair<hubConfigUrl(configFileName), null>
        val iconInfo: Pair<String?, String?>,
        val name: String?,
        val desc: String?,
        val extraData: ItemCardViewExtraData = ItemCardViewExtraData()
)