package net.xzos.upgradeall.ui.viewmodels.view

import net.xzos.upgradeall.core.server_manager.module.BaseApp

class ItemCardView internal constructor(
        val name: String? = null,
        val type: String? = null,
        val hubName: String? = null,
        val extraData: ItemCardViewExtraData = ItemCardViewExtraData()
) {
    fun isEmpty(): Boolean {
        return name == null
    }

    override fun equals(other: Any?): Boolean {
        return other is ItemCardView
                && other.name == name
                && other.type == type
                && other.hubName == hubName
                && other.extraData == extraData
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (hubName?.hashCode() ?: 0)
        result = 31 * result + extraData.hashCode()
        return result
    }
}

data class ItemCardViewExtraData(
        val app: BaseApp? = null,
        val uuid: String? = null
) {
    override fun equals(other: Any?): Boolean {
        return other is ItemCardViewExtraData
                && other.app == app
                && other.uuid == uuid
    }

    override fun hashCode(): Int {
        val appDatabase = app?.appDatabase
        val appId = appDatabase?.hubUuid + appDatabase?.id + appDatabase?.name
        var result = appId.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        return result
    }
}
