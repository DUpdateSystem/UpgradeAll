package net.xzos.upgradeall.data.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data.database.BaseAppDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.data.gson.UIConfig.Companion.APPLICATIONS_TYPE_TAG
import net.xzos.upgradeall.data.gson.UIConfig.Companion.APP_TYPE_TAG
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.USER_STAR_PAGE_INDEX
import net.xzos.upgradeall.utils.file.FileUtil
import java.io.File


/**
 * update_tab : {"name":"","icon":"","enable":"true"}
 * all_app_tab : {"name":"","icon":"","enable":"true"}
 * user_star_tab : {"name":"","icon":"","enable":"true","item_list":[{"type":"","name":"","icon":"","enable":"true","app_id_list":[0]}]}
 * user_tab_list : [{"name":"","icon":"","enable":"true","item_list":[{"type":"","name":"","icon":"","enable":"true","app_id_list":[0]}]}]
 */
data class UIConfig internal constructor(
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
                @SerializedName("app_id_list")
                var appIdList: MutableList<String> = mutableListOf()
        ) : BasicInfo(name) {

            override fun hashCode(): Int {
                var result = type.hashCode()
                result = 31 * result + name.hashCode()
                result = 31 * result + appIdList.hashCode()
                return result
            }

            override fun equals(other: Any?): Boolean {
                return when {
                    this === other -> true
                    javaClass != other?.javaClass -> false
                    else -> other.hashCode() == hashCode()
                }
            }
        }

        fun addItemList(itemList1: List<ItemListBean>) {
            val addUserTabList = itemList1.filter {
                !itemList.contains(it)
            }
            itemList.addAll(addUserTabList)
        }
    }

    fun addUserTab(name: String, icon: String?): Boolean {
        return if (name.isNotEmpty()) {
            userTabList.add(CustomContainerTabListBean(name).apply {
                this.icon = icon
            }).also { save() }
            true
        } else false
    }

    fun removeUserTab(position: Int? = null, userTabListBean: CustomContainerTabListBean? = null): Int {
        val itemPosition = position ?: userTabList.indexOf(userTabListBean)
        userTabList.removeAt(itemPosition).also { save() }
        return itemPosition
    }

    fun removeItemFromTabPage(position: Int, tabPageIndex: Int): Boolean {
        return popItemFromTabPage(position, tabPageIndex) != null
    }

    private fun popItemFromTabPage(position: Int, tabPageIndex: Int): CustomContainerTabListBean.ItemListBean? {
        val tabList = getTabList(tabPageIndex)?.itemList ?: return null
        return if (position < tabList.size) {
            return tabList[position].also {
                tabList.remove(it).also { save() }
            }
        } else null
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

    fun addItem(itemListBean: CustomContainerTabListBean.ItemListBean, tabPageIndex: Int): Boolean {
        val list = if (tabPageIndex == USER_STAR_PAGE_INDEX) {
            uiConfig.userStarTab
        } else uiConfig.userTabList[tabPageIndex]
        return list.itemList.add(itemListBean).also {
            if (it) save()
        }
    }

    fun moveItemToOtherGroup(position: Int, fromTabPageIndex: Int, toTabPageIndex: Int): Boolean {
        val item = popItemFromTabPage(position, fromTabPageIndex) ?: return false
        return addItem(item, toTabPageIndex)
    }

    private fun getTabList(position: Int): CustomContainerTabListBean? {
        return when {
            position == USER_STAR_PAGE_INDEX -> uiConfig.userStarTab
            position >= 0 -> uiConfig.userTabList[position]
            else -> null
        }
    }

    fun save() {
        FileUtil.UI_CONFIG_FILE.writeText(
                Gson().toJson(this)
        )
    }

    @Suppress("SENSELESS_COMPARISON")
    fun checkData(): Boolean {
        return updateTab != null && allAppTab != null && userStarTab != null && userTabList != null
    }

    companion object {
        private val context = MyApplication.context
        const val APP_TYPE_TAG = "app"
        const val APPLICATIONS_TYPE_TAG = "applications"
        val uiConfig: UIConfig = parseUiConfig(FileUtil.UI_CONFIG_FILE)
    }
}

private fun parseUiConfig(file: File): UIConfig {
    return try {
        parseUiConfig(file.readText())
    } catch (e: Throwable) {
        UIConfig()
    }
}

fun parseUiConfig(str: String): UIConfig {
    return try {
        val uiConfig = Gson().fromJson(str, UIConfig::class.java)
        if (uiConfig.checkData()) uiConfig else UIConfig()
    } catch (e: Throwable) {
        UIConfig()
    }
}

fun BaseAppDatabase.toItemListBean(): UIConfig.CustomContainerTabListBean.ItemListBean {
    val type = when (this) {
        is ApplicationsDatabase -> APPLICATIONS_TYPE_TAG
        else -> APP_TYPE_TAG
    }
    val idString = "$type-${id}"
    return UIConfig.CustomContainerTabListBean.ItemListBean(
            type, this.name, mutableListOf(idString)
    )
}

fun AppDatabase.toUiConfigId(): String {
    return "$APP_TYPE_TAG-$id"
}

fun ApplicationsDatabase.toUiConfigId(): String {
    return "$APPLICATIONS_TYPE_TAG-$id"
}

fun String.toDatabase(): BaseAppDatabase? {
    return try {
        val (type, id) = this.split("-", limit = 2)
        when (type) {
            APP_TYPE_TAG -> AppDatabaseManager.getAppDatabase(id.toLong())
            APPLICATIONS_TYPE_TAG -> AppDatabaseManager.getApplicationsDatabase(id.toLong())
            else -> null
        }
    } catch (ignore: IndexOutOfBoundsException) {
        null
    }
}

fun UIConfig.changeAppDatabaseId(databaseIdMap: Map<String, String>) {
    val changeItemListId = fun(list: MutableList<String>, databaseIdMap: Map<String, String>): MutableList<String> {
        for (i in list.indices) {
            val id = list[i]
            if (id in databaseIdMap.keys) {
                list.removeAt(i)
                list.add(i, databaseIdMap.getValue(id))
            }
        }
        return list
    }
    for (item in userStarTab.itemList)
        item.appIdList = changeItemListId(item.appIdList, databaseIdMap)
    for (userTab in userTabList)
        for (item in userTab.itemList)
            item.appIdList = changeItemListId(item.appIdList, databaseIdMap)
    save()
}
