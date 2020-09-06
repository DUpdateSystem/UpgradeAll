package net.xzos.upgradeall.core.data.database

abstract class BaseAppDatabase(
        var id: Long,
        var name: String,
        var hubUuid: String,
        var auth: Map<String, String?>,
        var extraId: Map<String, String?>
)
