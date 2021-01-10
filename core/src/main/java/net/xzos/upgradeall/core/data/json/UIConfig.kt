package net.xzos.upgradeall.core.data.json

import com.google.gson.Gson
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.utils.file.FileUtil
import java.io.File


/**
 * user_star : [{"group_name": "", "app_id_list": []}, ]
 */

const val USER_STAR_APP_GROUP = "USER_STAR_APP_GROUP"
const val USER_STAR_MAGISK_GROUP = "USER_STAR_MAGISK_GROUP"

val uiConfig: UIConfig = parseUiConfig(FileUtil.UI_CONFIG_FILE)

data class UIConfig(
        val user_star: MutableList<UserStar>
) {
    companion object {

        private fun Map<String, String?>.filter(groupName: String): Map<String, String?> {
            return when (groupName) {
                USER_STAR_APP_GROUP -> this.filter {
                    it.key == ANDROID_APP_TYPE
                }
                USER_STAR_MAGISK_GROUP -> this.filter {
                    it.key == ANDROID_MAGISK_MODULE_TYPE
                }
                else -> this
            }
        }

        fun MutableList<UserStar>.getAppIdList(groupName: String): List<Map<String, String?>> {
            for (item in this) {
                if (item.group_name == groupName) {
                    return item.app_id_list.toList()
                }
            }
            return emptyList()
        }

        fun MutableList<UserStar>.delAppId(groupName: String, _appId: Map<String, String?>) {
            val appId = _appId.filter(groupName)
            for (item in this) {
                if (item.group_name == groupName) {
                    val list = item.app_id_list
                    list.remove(appId)
                    if (list.isEmpty()) {
                        this.remove(item)
                    }
                }
            }
        }

        fun MutableList<UserStar>.addAppId(groupName: String, _appId: Map<String, String?>) {
            var userStarItem: UserStar? = null
            val appId = _appId.filter(groupName)
            for (item in this) {
                if (item.group_name == groupName) {
                    item.app_id_list.add(appId)
                    userStarItem = item
                }
            }
            if (userStarItem == null) {
                this.add(UserStar(groupName, hashSetOf(appId)))
            } else {
                userStarItem.app_id_list.add(appId)
            }
        }
    }
}

data class UserStar(
        val group_name: String,
        val app_id_list: HashSet<Map<String, String?>>,
)

private fun parseUiConfig(file: File): UIConfig {
    return try {
        parseUiConfig(file.readText())
    } catch (e: Throwable) {
        UIConfig(mutableListOf())
    }
}

fun parseUiConfig(str: String): UIConfig {
    return try {
        Gson().fromJson(str, UIConfig::class.java)
    } catch (e: Throwable) {
        UIConfig(mutableListOf())
    }
}


fun UIConfig.save() {
    FileUtil.UI_CONFIG_FILE.writeText(
            Gson().toJson(this)
    )
}