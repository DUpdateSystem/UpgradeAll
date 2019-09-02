package net.xzos.upgradeAll.json.gson

import com.google.gson.annotations.SerializedName

data class HubDatabaseExtraData(
        @SerializedName("javascript") var javascript: String? = null
)
