package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.database.manager.CloudConfigGetter
import net.xzos.upgradeAll.data.json.gson.CloudConfig
import net.xzos.upgradeAll.data.json.nongson.ItemCardViewExtraData
import net.xzos.upgradeAll.ui.viewmodels.adapters.CloudAppItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.adapters.CloudHubItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView

internal class CloudConfigPlaceholderFragment : Fragment() {

    private var pageModelIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageModelIndex = arguments?.getInt(ARG_SECTION_NUMBER) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.content_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener { this.renewCardView() }
    }

    override fun onResume() {
        super.onResume()
        renewCardView()
    }

    private fun renewCardView() {
        GlobalScope.launch {
            launch(Dispatchers.Main) { swipeRefreshLayout?.isRefreshing = true }
            if (pageModelIndex == CLOUD_APP_CONFIG)
                renewCloudList(isAppList = true)
            else if (pageModelIndex == CLOUD_HUB_CONFIG)
                renewCloudList(isHubList = true)
            launch(Dispatchers.Main) { swipeRefreshLayout?.isRefreshing = false }
        }
    }

    private fun renewCloudList(isAppList: Boolean = false, isHubList: Boolean = false) {
        runBlocking {
            when {
                isAppList -> {
                    CloudConfigGetter.appList?.map { getCloudAppItemCardView(it) }
                }
                isHubList -> {
                    CloudConfigGetter.hubList?.map { getCloudHubItemCardView(it) }
                }
                else -> null
            }?.plus(ItemCardView(
                    Pair(null, null),
                    null,
                    null,
                    extraData = ItemCardViewExtraData(isEmpty = true)))
                    ?.apply {
                        launch(Dispatchers.Main) {
                            if (this@CloudConfigPlaceholderFragment.isVisible) {
                                cardItemRecyclerView?.let { view ->
                                    view.layoutManager = GridLayoutManager(activity, 1)
                                    view.adapter = when {
                                        isAppList -> CloudAppItemAdapter(this@apply, context)
                                        isHubList -> CloudHubItemAdapter(this@apply)
                                        else -> null
                                    }
                                }
                            }
                        }
                    }
                    ?: launch(Dispatchers.Main) {
                        activity?.let {
                            Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                        }
                    }
        }
    }

    private fun getCloudAppItemCardView(item: CloudConfig.AppListBean): ItemCardView {
        val name = item.appConfigName
        // TODO: 提醒用户使用 dev 分支，正式版删除
        if (name == null) {
            runBlocking(Dispatchers.Main) { Toast.makeText(MyApplication.context, "请使用云端配置仓库的 dev 分支", Toast.LENGTH_LONG).show() }
            return ItemCardView(Pair(null, null), null, null, extraData = ItemCardViewExtraData(isEmpty = true))
        }
        val appUuid = item.appConfigUuid
        val configFileName = item.appConfigFileName
        val iconInfo: Pair<String?, String?> = Pair(configFileName, null)
        return ItemCardView(iconInfo, name, appUuid, ItemCardViewExtraData(uuid = appUuid))
    }

    private fun getCloudHubItemCardView(item: CloudConfig.HubListBean): ItemCardView {
        val name = item.hubConfigName
        val hubUuid = item.hubConfigUuid
        val configFileName = item.hubConfigFileName
        val iconInfo: Pair<String?, String?> = Pair(configFileName, null)
        return ItemCardView(iconInfo, name, hubUuid, ItemCardViewExtraData(uuid = hubUuid))
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "cloudConfigTag"

        internal const val CLOUD_APP_CONFIG = 0
        internal const val CLOUD_HUB_CONFIG = 1

        internal fun newInstance(pageIndex: Int): CloudConfigPlaceholderFragment {
            val fragment = CloudConfigPlaceholderFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_SECTION_NUMBER, pageIndex)
            fragment.arguments = bundle
            return fragment
        }
    }
}