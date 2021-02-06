package net.xzos.upgradeall.core.data.json

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.upgradeall.core.utils.file.FileUtil
import java.io.File


/**
 * user_star : [{}]
 */

val uiConfig: UIConfig = parseUiConfig(FileUtil.UI_CONFIG_FILE)

data class UIConfig(
        @SerializedName("user_star_app_id_list") val userStarAppIdList: HashSet<Map<String, String?>>
)

private fun parseUiConfig(file: File): UIConfig {
    return try {
        parseUiConfig(file.readText())
    } catch (e: Throwable) {
        UIConfig(hashSetOf())
    }
}

fun parseUiConfig(str: String): UIConfig {
    return try {
        Gson().fromJson(str, UIConfig::class.java)
    } catch (e: Throwable) {
        UIConfig(hashSetOf())
    }
}


fun UIConfig.save() {
    FileUtil.UI_CONFIG_FILE.writeText(
            Gson().toJson(this)
    )
}