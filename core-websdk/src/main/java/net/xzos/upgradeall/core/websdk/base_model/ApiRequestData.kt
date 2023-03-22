package net.xzos.upgradeall.core.websdk.base_model


abstract class ApiRequestData(
    val hubUuid: String,
    val auth: Map<String, String?>,
    val other: Map<String, String?>,
)

class SingleRequestData(
    hubUuid: String,
    auth: Map<String, String?>,
    val appId: Map<String, String?>,
    other: Map<String, String?>,
) : ApiRequestData(hubUuid, auth, other)

class MultiRequestData(
    hubUuid: String,
    auth: Map<String, String?>,
    val appIdList: List<Map<String, String?>>,
    other: Map<String, String?>,
) : ApiRequestData(hubUuid, auth, other)