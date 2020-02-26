package net.xzos.upgradeall.data_manager

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.dupdatesystem.core.data.database.AppDatabase
import net.xzos.dupdatesystem.core.server_manager.runtime.manager.module.app.App
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.USER_STAR_PAGE_INDEX
import net.xzos.upgradeall.utils.FileUtil


/**
 * update_tab : {"name":"","icon":"","enable":"true"}
 * all_app_tab : {"name":"","icon":"","enable":"true"}
 * user_star_tab : {"name":"","icon":"","enable":"true","item_list":[{"type":"","name":"","icon":"","enable":"true","app_id_list":[0]}]}
 * user_tab_list : [{"name":"","icon":"","enable":"true","item_list":[{"type":"","name":"","icon":"","enable":"true","app_id_list":[0]}]}]
 */
class UIConfig(
        @SerializedName("update_tab") var updateTab: PresetTabBean = PresetTabBean(context.getString(R.string.update)).apply {
            icon = FileUtil.UPDATE_TAB_IMAGE_NAME
        },
        @SerializedName("all_app_tab") var allAppTab: PresetTabBean = PresetTabBean(context.getString(R.string.all_app)).apply {
            icon = FileUtil.ALL_APP_TAB_IMAGE_NAME
        },
        @SerializedName("user_star_tab") var userStarTab: CustomContainerTabListBean =
                CustomContainerTabListBean(context.getString(R.string.user_star)).apply {
                    icon = FileUtil.USER_STAR_TAB_IMAGE_NAME
                },
        @SerializedName("user_tab_list") var userTabList: MutableList<CustomContainerTabListBean> = mutableListOf()
) {

    /**
     * name :
     * icon :
     * enable : true
     */
    open class BasicInfo(
            @SerializedName("name") @Transient open var name: String
    ) {
        @SerializedName("icon")
        var icon: String? = null
        @SerializedName("enable")
        var enable: Boolean = true
    }

    /**
     * name :
     * icon :
     * enable : true
     */
    class PresetTabBean(
            @SerializedName("name") override var name: String
    ) : BasicInfo(name)

    /**
     * name :
     * icon :
     * enable : true
     * item_list : [{"type":"","name":"","icon":"","enable":"true","app_id_list":[0]}]
     */
    class CustomContainerTabListBean(
            @SerializedName("name") override var name: String,
            @SerializedName("item_list") var itemList: MutableList<ItemListBean> = mutableListOf()
    ) : BasicInfo(name) {

        /**
         * type :
         * name :
         * icon :
         * enable : true
         * app_id_list : [0]
         */
        class ItemListBean(
                @SerializedName("type") var type: String,
                @SerializedName("name") override var name: String,
                @SerializedName("app_id_list") var appIdList: MutableList<Long> = mutableListOf()
        ) : BasicInfo(name)
    }

    fun addUserTab(name: String, icon: String?): Boolean {
        return if (name.isNotEmpty()) {
            userTabList.add(CustomContainerTabListBean(name).apply {
                this.icon = icon
            })
            save()
            true
        } else false
    }

    fun removeUserTab(position: Int? = null, userTabListBean: CustomContainerTabListBean? = null) {
        if (position != null) userTabList.removeAt(position)
        else if (userTabListBean != null) userTabList.remove(userTabListBean)
        save()
    }

    fun swapUserTabOrder(fromPosition: Int, toPosition: Int): Boolean {
        return if (fromPosition >= 0 && fromPosition < userTabList.size
                && toPosition >= 0 && toPosition < userTabList.size) {
            userTabList[fromPosition] = userTabList[toPosition]
                    .also { userTabList[toPosition] = userTabList[fromPosition] }
            save()
            true
        } else false
    }

    fun moveItemToOtherGroup(position: Int, fromTabPageIndex: Int,
                             toTabPageIndex: Int? = null,
                             containerTabListBean: CustomContainerTabListBean? = null,
                             app: App? = null
    ): Boolean {
        if (toTabPageIndex != null || containerTabListBean != null) {
            if (toTabPageIndex != UPDATE_PAGE_INDEX && toTabPageIndex != ALL_APP_PAGE_INDEX) {
                val item =
                        if (fromTabPageIndex != UPDATE_PAGE_INDEX && fromTabPageIndex != ALL_APP_PAGE_INDEX) {
                            if (fromTabPageIndex == USER_STAR_PAGE_INDEX) {
                                uiConfig.userStarTab.itemList.removeAt(position)
                            } else {
                                uiConfig.userTabList[fromTabPageIndex].itemList.removeAt(position)
                            }
                        } else if (app != null) {
                            CustomContainerTabListBean.ItemListBean(APP_TYPE_TAG, app.appInfo.name, mutableListOf(app.appInfo.id))
                        } else return false
                if (containerTabListBean != null) {
                    containerTabListBean.itemList.add(item)
                } else {
                    when {
                        toTabPageIndex == USER_STAR_PAGE_INDEX -> uiConfig.userStarTab.itemList.add(item)
                        toTabPageIndex != null -> uiConfig.userTabList[toTabPageIndex].itemList.add(item)
                        else -> return false
                    }
                }
                save()
                return true
            }
        }
        return false
    }

    fun save() {
        FileUtil.UI_CONFIG_FILE.writeText(
                Gson().toJson(this)
        )
    }

    companion object {
        val uiConfig: UIConfig = try {
            Gson().fromJson(FileUtil.UI_CONFIG_FILE.readText(), UIConfig::class.java)
        } catch (e: Throwable) {
            UIConfig()
        }

        @Transient
        internal const val APP_TYPE_TAG = AppDatabase.APP_TYPE_TAG
        @Transient
        internal const val GROUP_TYPE_TAG = "group"
        @Transient
        internal const val APPLICATIONS_TYPE_TAG = AppDatabase.APPLICATIONS_TYPE_TAG
    }
}
