package net.xzos.upgradeall.data.gson

import com.google.gson.annotations.SerializedName

data class ExtraItem(
        @SerializedName("key") var key: String = "",
        @SerializedName("value") var value: String = ""
)