package net.xzos.upgradeall.ui.fragment.cloud_config

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_APP_PACKAGE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_MAGISK_MODULE
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson.Companion.API_TYPE_SHELL_ROOT
import net.xzos.upgradeall.core.data_manager.*
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.ui.viewmodels.adapters.CloudAppItemAdapter
import net.xzos.upgradeall.ui.viewmodels.adapters.CloudHubItemAdapter
import net.xzos.upgradeall.ui.viewmodels.adapters.CloudItemAdapter
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.utils.MiscellaneousUtils

internal class CloudConfigPlaceholderFragment : Fragment(), SearchView.OnQueryTextListener {

    private var pageModelIndex = 0
    private lateinit var adapter: CloudItemAdapter
    private var menu: Menu? = null
    private var isListReady = false
    private var itemCardViewList: List<CloudConfigListItemView> = listOf()

    init {
        renewConfig()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageModelIndex = arguments?.getInt(ARG_SECTION_NUMBER) ?: 0
        lifecycleScope.launchWhenStarted {
            renewCardView()
        }
    }

    private fun renewConfig() {
        lifecycleScope.launch {
            if (PreferencesMap.auto_update_app_config)
                AppDatabaseManager.renewAllAppConfigFromCloud()
            if (PreferencesMap.auto_update_hub_config)
                HubDatabaseManager.renewAllHubConfigFromCloud()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.content_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener { this.renewCardView() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_cloud_app_list, menu)
        this.menu = menu

        val searchView = SearchView(requireContext()).apply {
            setIconifiedByDefault(false)
            setOnQueryTextListener(this@CloudConfigPlaceholderFragment)
            queryHint = getText(R.string.menu_search_hint)
            isQueryRefinementEnabled = true

            findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
        }

        menu.findItem(R.id.search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView

            if (!isListReady) {
                isVisible = false
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun renewCardView() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) { swipeRefreshLayout?.isRefreshing = true }
            withContext(Dispatchers.IO) {
                CloudConfigGetter.renew()
            }
            if (pageModelIndex == CLOUD_APP_CONFIG) {
                renewCloudList(isAppList = true)
            } else if (pageModelIndex == CLOUD_HUB_CONFIG) {
                renewCloudList(isHubList = true)
            }
            withContext(Dispatchers.Main) {
                swipeRefreshLayout?.isRefreshing = false
                isListReady = true
                menu?.findItem(R.id.search)?.isVisible = true
            }
        }
    }

    private suspend fun renewCloudList(isAppList: Boolean = false, isHubList: Boolean = false) {
        itemCardViewList = (when {
            isAppList -> {
                CloudConfigGetter.appConfigList?.map { getCloudAppItemCardView(it) }
            }
            isHubList -> {
                CloudConfigGetter.hubConfigList?.map { getCloudHubItemCardView(it) }
            }
            else -> null
        } ?: listOf<CloudConfigListItemView>().also {
            withContext(Dispatchers.Main) {
                if (this@CloudConfigPlaceholderFragment.isVisible) {
                    MiscellaneousUtils.showToast(R.string.network_error)
                }
            }
        }).plus(CloudConfigListItemView())

        withContext(Dispatchers.Main) {
            if (this@CloudConfigPlaceholderFragment.isVisible) {
                cardItemRecyclerView?.let { view ->
                    view.layoutManager = LinearLayoutManager(requireContext())
                    adapter = when {
                        isAppList -> CloudAppItemAdapter(itemCardViewList, context).apply { setHasStableIds(true) }
                        isHubList -> CloudHubItemAdapter(itemCardViewList).apply { setHasStableIds(true) }
                        else -> throw IllegalArgumentException("wrong argument")
                    }
                    view.adapter = adapter
                }
            }
        }
    }

    private fun getCloudAppItemCardView(appConfig: AppConfigGson): CloudConfigListItemView {
        val name = appConfig.info.appName
        val appUuid = appConfig.uuid
        val appCloudConfig = CloudConfigGetter.getAppCloudConfig(appUuid)
        val type: Int? = when (appCloudConfig?.appConfig?.targetChecker?.api?.toLowerCase(AppValue.locale)) {
            API_TYPE_APP_PACKAGE -> R.string.android_app
            API_TYPE_MAGISK_MODULE -> R.string.magisk_module
            API_TYPE_SHELL -> R.string.shell
            API_TYPE_SHELL_ROOT -> R.string.shell_root
            else -> null
        }
        val hubUuid = appCloudConfig?.appConfig?.hubInfo?.hubUuid
        val hubName = CloudConfigGetter.getHubCloudConfig(hubUuid)?.info?.hubName
        return CloudConfigListItemView(name, type, hubName, appUuid)
    }

    private fun getCloudHubItemCardView(item: HubConfigGson): CloudConfigListItemView {
        val name = item.info.hubName
        val hubUuid = item.uuid
        val hubCloudConfig = CloudConfigGetter.getHubCloudConfig(hubUuid)
        val type = if (hubCloudConfig?.apiKeywords?.contains("android_app_package") == true) {
            R.string.applications
        } else {
            R.string.app_hub
        }
        return CloudConfigListItemView(name, type, hubUuid, hubUuid)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val filter = itemCardViewList.filter { it.name.contains(newText, ignoreCase = true) }
        adapter.mItemCardViewList = filter
        adapter.notifyDataSetChanged()
        return false
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "CLOUD_CONFIG_TAG"

        internal const val CLOUD_APP_CONFIG = 0
        internal const val CLOUD_HUB_CONFIG = 1

        internal fun newInstance(pageIndex: Int): CloudConfigPlaceholderFragment {
            return CloudConfigPlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, pageIndex)
                }
            }
        }
    }
}
