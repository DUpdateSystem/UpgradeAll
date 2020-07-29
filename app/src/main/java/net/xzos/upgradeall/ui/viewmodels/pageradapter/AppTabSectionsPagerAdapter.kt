package net.xzos.upgradeall.ui.viewmodels.pageradapter

import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.group_item.view.*
import kotlinx.android.synthetic.main.group_item.view.groupCardView
import kotlinx.android.synthetic.main.group_item.view.groupIconImageView
import kotlinx.android.synthetic.main.view_add_group_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.data.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.ui.activity.file_pref.UCropActivity
import net.xzos.upgradeall.ui.fragment.app_list.page_view.AppListPlaceholderFragment
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.MiscellaneousUtils
import java.io.File

class AppTabSectionsPagerAdapter(private val tabLayout: TabLayout, fm: FragmentManager,
                                 private val lifecycleOwner: LifecycleOwner) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private var mTabIndexList: MutableList<Int> = initTabIndexList()

    init {
        // 设置添加按钮自动弹出
        editTabMode.observe(lifecycleOwner, Observer { editTabMode ->
            val tabIndexList: MutableList<Int>
            when {
                editTabMode -> {
                    mTabIndexList = getAllTabIndexList()
                    notifyDataSetChanged()
                }
                initTabIndexList().also { tabIndexList = it }
                        .isNotEmpty() -> {
                    mTabIndexList = tabIndexList
                    checkUpdate()
                    notifyDataSetChanged()
                }
                else -> {
                    // 尝试阻止用户退出编辑模式
                    MiscellaneousUtils.showToast(
                            tabLayout.context,
                            R.string.please_do_not_hide_all_bookmark_page,
                            duration = Toast.LENGTH_LONG)
                    AppTabSectionsPagerAdapter.editTabMode.value = true
                }
            }
        })
        AppUiDataManager.getAppListLivaData(UPDATE_PAGE_INDEX).observe(lifecycleOwner, Observer {
            checkUpdate()
        })
    }

    // 检查更新页面是否刷新
    private fun checkUpdate() {
        val needUpdateAppList: List<BaseApp> = AppUiDataManager.getAppListLivaData(UPDATE_PAGE_INDEX).value
                ?: return
        when {
            !UpdateManager.isRunning
                    && needUpdateAppList.isEmpty()
                    && editTabMode.value == false ->
                removeTabPage(mTabIndexList.indexOf(UPDATE_PAGE_INDEX))
            needUpdateAppList.isNotEmpty() ->
                addTabPage(UPDATE_PAGE_INDEX, 0)
        }
    }

    override fun getItem(position: Int): Fragment {
        checkUpdate()
        val tabIndex =
                if (position < mTabIndexList.size)
                    mTabIndexList[position]
                else mTabIndexList.last()
        return if (tabIndex == ADD_TAB_BUTTON_INDEX) Fragment()
        else AppListPlaceholderFragment.newInstance(tabIndex)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        // 传递 hubUuid
        return if (position < mTabIndexList.size)
            mTabIndexList[position].toString()
        else mTabIndexList.last().toString()
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getCount(): Int {
        return mTabIndexList.size
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        for (i in 0 until tabLayout.tabCount)
            tabLayout.getTabAt(i)?.customView = getCustomTabView(i)
    }

    private fun getAllTabIndexList(): MutableList<Int> = mutableListOf(
            UPDATE_PAGE_INDEX, USER_STAR_PAGE_INDEX,
            ALL_APP_PAGE_INDEX, ADD_TAB_BUTTON_INDEX).apply {
        // 在 ALL_APP_PAGE 后插入 uiConfig 索引
        this.addAll(this.indexOf(ALL_APP_PAGE_INDEX),
                (0 until uiConfig.userTabList.size).toList())
    }

    private fun initTabIndexList(): MutableList<Int> {
        val tabIndexList = mutableListOf<Int>()
        if (uiConfig.updateTab.enable)
            tabIndexList.add(UPDATE_PAGE_INDEX)
        if (uiConfig.userStarTab.enable)
            tabIndexList.add(USER_STAR_PAGE_INDEX)
        for (i in uiConfig.userTabList.indices) {
            val userTab = uiConfig.userTabList[i]
            if (userTab.enable)
                tabIndexList.add(i)
        }
        if (uiConfig.allAppTab.enable)
            tabIndexList.add(ALL_APP_PAGE_INDEX)
        return tabIndexList
    }

    private fun addTabPage(tabIndex: Int, position: Int = mTabIndexList.size) {
        if (!mTabIndexList.contains(tabIndex)) {
            mTabIndexList.add(position, tabIndex)
            notifyDataSetChanged()
        }
    }

    private fun removeTabPage(position: Int) {
        if (position >= 0 && position < mTabIndexList.size) {
            mTabIndexList.removeAt(position)
            notifyDataSetChanged()
        }
    }

    private fun swapTabPage(position: Int, targetPosition: Int) {
        if (position >= 0 && position < mTabIndexList.size && targetPosition >= 0 && targetPosition < mTabIndexList.size)
            mTabIndexList[position] = mTabIndexList[targetPosition].also { mTabIndexList[targetPosition] = mTabIndexList[position] }
        notifyDataSetChanged()
    }

    private fun getProgressBarFromCustomTabView(position: Int): ProgressBar? =
            tabLayout.getTabAt(position)?.customView?.loadingBar

    private fun loadGroupViewAndReturnBasicInfo(
            tabIndex: Int,
            groupIconImageView: ImageView,
            textView: View
    ): UIConfig.BasicInfo {
        val tabIconDrawableId = getTabIconDrawableId(tabIndex)
        val tabBasicInfo = when (tabIndex) {
            UPDATE_PAGE_INDEX -> {
                uiConfig.updateTab
            }
            USER_STAR_PAGE_INDEX -> {
                uiConfig.userStarTab
            }
            ALL_APP_PAGE_INDEX -> {
                uiConfig.allAppTab
            }
            else -> {
                uiConfig.userTabList[tabIndex]
            }
        }
        val name = tabBasicInfo.name
        val icon = tabBasicInfo.icon.toString()
        if (textView is TextView) {
            textView.text = name
        } else if (textView is EditText) {
            textView.setText(name)
        }
        IconPalette.loadHubIconView(
                iconImageView = groupIconImageView,
                file = File(FileUtil.GROUP_IMAGE_DIR, icon),
                hubIconDrawableId = tabIconDrawableId
        )
        return tabBasicInfo
    }

    private fun getTabIconDrawableId(tabIndex: Int): Int? {
        return when (tabIndex) {
            UPDATE_PAGE_INDEX -> {
                R.drawable.ic_update
            }
            USER_STAR_PAGE_INDEX -> {
                R.drawable.ic_start
            }
            ALL_APP_PAGE_INDEX -> {
                R.drawable.ic_apps
            }
            else -> null
        }
    }

    private fun getCustomTabView(position: Int): View {
        return LayoutInflater.from(tabLayout.context)
                .inflate(R.layout.group_item, tabLayout, false).apply {
                    // 通用 UI 组件初始化
                    loadingBar.visibility = View.GONE
                    editGroupCardView.visibility = View.GONE
                    // 初始化按钮相应
                    setCustomTabViewClickListener(this, position)
                    // 非按钮 Tab 初始化
                    if (position >= 0 && position < mTabIndexList.size) {
                        when (val tabIndex = mTabIndexList[position]) {
                            ADD_TAB_BUTTON_INDEX -> {
                                groupCardView.visibility = View.GONE
                                addGroupCardView.visibility = View.VISIBLE
                                setOnClickListener {
                                    showAddGroupAlertDialog(this, position)
                                }
                                return@apply
                            }
                            else -> loadGroupViewAndReturnBasicInfo(tabIndex, groupIconImageView, groupNameTextView)
                        }
                        groupCardView.visibility = View.VISIBLE
                        addGroupCardView.visibility = View.GONE
                        editTabMode.observe(lifecycleOwner, Observer { editTabMode ->
                            editGroupCardView.visibility = if (editTabMode) View.VISIBLE else View.GONE
                        })
                    }
                }
    }

    private fun setCustomTabViewClickListener(view: View, position: Int) {
        with(view) {
            setOnLongClickListener {
                editTabMode.value = true
                return@setOnLongClickListener true
            }
            setOnClickListener(View.OnClickListener {
                if (tabLayout.selectedTabPosition != position) {
                    tabLayout.getTabAt(position)?.select()
                }
                if (editTabMode.value == true)
                    editTabMode.value = false
                return@OnClickListener
            })
            editGroupCardView.setOnClickListener {
                showAddGroupAlertDialog(this, position)
            }
        }
    }

    private fun showAddGroupAlertDialog(view: View, position: Int) {
        val context = view.context
        AlertDialog.Builder(context).also {
            var cacheImageFile: File? = null
            var tabBasicInfo: UIConfig.BasicInfo? = null
            val dialogView = LayoutInflater.from(context)
                    .inflate(R.layout.view_add_group_item, tabLayout, false).apply {
                        val tabIndex = mTabIndexList[position]
                        if (tabIndex != ADD_TAB_BUTTON_INDEX) {
                            it.setMessage(R.string.edit_group)
                            tabBasicInfo =
                                    loadGroupViewAndReturnBasicInfo(tabIndex, groupIconImageView, groupNameEditText)
                            cacheImageFile = FileUtil.getUserGroupIcon(tabBasicInfo!!.icon)
                            if (tabIndex >= 0) {
                                it.setNeutralButton(R.string.delete) { dialog, _ ->
                                    AppUiDataManager.removeUserTab(position = tabIndex)
                                    mTabIndexList = getAllTabIndexList()
                                    notifyDataSetChanged()
                                    dialog.cancel()
                                }
                                uiConfig.userTabList[tabIndex]
                                liftMoveImageView.setOnClickListener {
                                    if (AppUiDataManager.swapUserTabOrder(tabIndex, tabIndex - 1)) {
                                        swapTabPage(position, position - 1)
                                        notifyDataSetChanged()
                                    }
                                }
                                rightMoveImageView.setOnClickListener {
                                    if (AppUiDataManager.swapUserTabOrder(tabIndex, tabIndex + 1)) {
                                        swapTabPage(position, position + 1)
                                        notifyDataSetChanged()
                                    }
                                }
                            }
                            it.setNegativeButton(
                                    if (tabBasicInfo!!.enable) R.string.hide
                                    else R.string.unhide
                            ) { dialog, _ ->
                                tabBasicInfo!!.enable = !tabBasicInfo!!.enable
                                dialog.cancel()
                            }
                        } else {
                            it.setMessage(R.string.add_new_group)
                        }
                        if (tabIndex < 0) {
                            liftMoveImageView.visibility = View.GONE
                            rightMoveImageView.visibility = View.GONE
                        }
                        groupCardView.setOnLongClickListener {
                            tabBasicInfo?.icon = null
                            val tabIconDrawableId = getTabIconDrawableId(tabIndex)
                            IconPalette.loadHubIconView(
                                    iconImageView = groupIconImageView,
                                    hubIconDrawableId = tabIconDrawableId
                            )
                            IconPalette.loadHubIconView(
                                    iconImageView = view.groupIconImageView,
                                    hubIconDrawableId = tabIconDrawableId
                            )
                            return@setOnLongClickListener true
                        }
                        groupCardView.setOnClickListener {
                            GlobalScope.launch {
                                if (cacheImageFile == null)
                                    cacheImageFile = FileUtil.getNewRandomNameFile(FileUtil.GROUP_IMAGE_DIR)
                                if (UCropActivity.newInstance(1f, 1f, cacheImageFile!!, context)) {
                                    withContext(Dispatchers.Main) {
                                        IconPalette.loadHubIconView(
                                                iconImageView = groupIconImageView,
                                                file = cacheImageFile
                                        )
                                        IconPalette.loadHubIconView(
                                                iconImageView = view.groupIconImageView,
                                                file = cacheImageFile
                                        )
                                    }
                                }
                            }
                        }
                    }
            it.setView(dialogView)
            it.setPositiveButton(R.string.ok) { dialog, _ ->
                val name = dialogView.groupNameEditText.text.toString()
                if (name.isNotEmpty()) {
                    if (tabBasicInfo != null) {
                        tabBasicInfo?.name = name
                        uiConfig.save()
                    } else {
                        if (AppUiDataManager.addUserTab(name, cacheImageFile?.name)) {
                            addTabPage(uiConfig.userTabList.size - 1)
                            editTabMode.value = false
                        }
                    }
                    dialog.cancel()
                } else {
                    MiscellaneousUtils.showToast(context, R.string.please_input_name, duration = Toast.LENGTH_LONG)
                }
            }
        }.create().show()
    }

    companion object {
        const val ADD_TAB_BUTTON_INDEX = -4
        const val UPDATE_PAGE_INDEX = -3
        const val USER_STAR_PAGE_INDEX = -2
        const val ALL_APP_PAGE_INDEX = -1

        val editTabMode = MutableLiveData(false)

        fun newInstance(
                tabLayout: TabLayout,
                viewPager: ViewPager,
                childFragmentManager: FragmentManager,
                lifecycleOwner: LifecycleOwner
        ) {
            val sectionsPagerAdapter =
                    AppTabSectionsPagerAdapter(tabLayout, childFragmentManager, lifecycleOwner)
            viewPager.adapter = sectionsPagerAdapter
            with(tabLayout) {
                this.setupWithViewPager(viewPager)
            }
        }
    }
}
