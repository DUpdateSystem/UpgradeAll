package net.xzos.upgradeall.core.websdk.base_model


data class HubData(
    val hubUuid: String,
    val auth: Map<String, String?>,
    val other: Map<String, String?> = mapOf(),
) {
    fun getStringId(): String = "$hubUuid/${auth.getString()}/${other.getString()}"
}

data class AppData(
    val appId: Map<String, String?>,
    val other: Map<String, String?>,
) {
    fun getStringId(): String = "${appId.getString()}/${other.getString()}"
}

abstract class ApiRequestData(
    val hub: HubData,
)

class SingleRequestData(
    hub: HubData,
    val app: AppData,
) : ApiRequestData(hub)

class MultiRequestData(
    hub: HubData,
    val appList: Collection<AppData>,
) : ApiRequestData(hub)

fun Map<String, String?>.getString() = entries.joinToString { "${it.key}:${it.value}" }