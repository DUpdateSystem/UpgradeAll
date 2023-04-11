package net.xzos.upgradeall.core.websdk.base_model


data class HubData(
    val hubUuid: String,
    val auth: Map<String, String?>,
    val other: Map<String, String?> = mapOf(),
) {
    fun getStringId(): String = "$hubUuid|${auth.getString()}|${other.getString()}"
}

data class AppData(
    val appId: Map<String, String?>,
    val other: Map<String, String?>,
) {
    fun getStringId(): String = "${appId.getString()}|${other.getString()}"
}

interface ApiRequestData {
    val hub: HubData
}

data class SingleRequestData(
    override val hub: HubData,
    val app: AppData,
) : ApiRequestData

data class MultiRequestData(
    override val hub: HubData,
    val appList: Collection<AppData>,
) : ApiRequestData

fun Map<String, String?>.getString() = entries.joinToString { "${it.key}:${it.value}" }