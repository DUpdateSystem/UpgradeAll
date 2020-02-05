package net.xzos.upgradeAll.data.json.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.upgradeAll.utils.FileUtil

/**
 * user_star_tab : {"user_star_enable":"true","item_list":[{"type":"","name":"","icon":"","app_id_list":[0]}]}
 * user_tab_list : [{"name":"","icon":"","item_list":[{"type":"","name":"","icon":"","app_id_list":[0]}]}]
 */
internal data class UIConfig(
        @SerializedName("user_star_tab") var userStarTab: UserStarTabBean = UserStarTabBean(),
        @SerializedName("user_tab_list") var userTabList: MutableList<UserTabListBean> = mutableListOf()

) {

    /**
     * user_star_enable : true
     * item_list : [{"type":"","name":"","icon":"","app_id_list":[0]}]
     */
    data class UserStarTabBean(
            @SerializedName("user_star_enable") var userStarEnable: Boolean = true,
            @SerializedName("item_list") var itemList: MutableList<ItemListBean> = mutableListOf()
    )

    /**
     * name:
     * icon:
     * item_list: [{"type": "", "name": "", "icon": "", "app_id_list": [0]}]
     */
    data class UserTabListBean(
            @SerializedName("name") var name: String,
            @SerializedName("icon") var icon: String,
            @SerializedName("item_list") var itemList: MutableList<ItemListBean> = mutableListOf()
    ) {

        /**
         * type:
         * name:
         * icon:
         * app_id_list: [0]
         */
    }

    data class ItemListBean(
            @SerializedName("type") var type: String,
            @SerializedName("name") var name: String,
            @SerializedName("icon") var icon: String,
            @SerializedName("app_id_list") var appIdList: MutableList<Int> = mutableListOf()
    )

    companion object {
        val uiConfig = getConfig()

        private fun getConfig(): UIConfig {
            return try {
                Gson().fromJson(FileUtil.UI_CONFIG_FILE.readText(), UIConfig::class.java)
            } catch (e: Throwable) {
                UIConfig()
            }
        }

        fun save(uiConfig: UIConfig) {
            FileUtil.UI_CONFIG_FILE.writeText(
                    Gson().toJson(uiConfig)
            )
        }
    }
}
