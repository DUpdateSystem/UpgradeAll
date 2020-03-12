package net.xzos.upgradeall.ui.viewmodels.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.dupdatesystem.core.data.config.AppConfig
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_APP_PACKAGE
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_MAGISK_MODULE
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_SHELL
import net.xzos.dupdatesystem.core.data.json.gson.AppConfigGson.AppConfigBean.TargetCheckerBean.Companion.API_TYPE_SHELL_ROOT
import net.xzos.dupdatesystem.core.data.json.gson.CloudConfig
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.ui.viewmodels.adapters.CloudAppItemAdapter
import net.xzos.upgradeall.ui.viewmodels.adapters.CloudHubItemAdapter
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.utils.MiscellaneousUtils

internal class CloudConfigPlaceholderFragment : Fragment() {

    private val cloudConfigGetter = MiscellaneousUtils.cloudConfigGetter

    private var pageModelIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageModelIndex = arguments?.getInt(ARG_SECTION_NUMBER) ?: 0
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.content_list, container, false)

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
            val cloudConfigGetter = cloudConfigGetter
            when {
                isAppList -> {
                    cloudConfigGetter.appList?.map { getCloudAppItemCardView(it) }
                }
                isHubList -> {
                    cloudConfigGetter.hubList?.map { getCloudHubItemCardView(it) }
                }
                else -> null
            }?.plus(CloudConfigListItemView.newEmptyInstance())
                    ?.also {
                        launch(Dispatchers.Main) {
                            if (this@CloudConfigPlaceholderFragment.isVisible) {
                                cardItemRecyclerView?.let { view ->
                                    view.layoutManager = GridLayoutManager(activity, 1)
                                    view.adapter = when {
                                        isAppList -> CloudAppItemAdapter(it, context)
                                        isHubList -> CloudHubItemAdapter(it)
                                        else -> null
                                    }
                                }
                            }
                        }
                    }
                    ?: {
                        if (this@CloudConfigPlaceholderFragment.isVisible)
                            MiscellaneousUtils.showToast(context, R.string.network_error)
                    }
        }
    }

    private fun getCloudAppItemCardView(item: CloudConfig.AppListBean): CloudConfigListItemView {
        val name = item.appConfigName
        val appUuid = item.appConfigUuid
        val appCloudConfig = cloudConfigGetter.getAppCloudConfig(appUuid)
        val type: String
        with(MyApplication.context) {
            type = when (appCloudConfig?.appConfig?.targetChecker?.api?.toLowerCase(AppConfig.locale)) {
                API_TYPE_APP_PACKAGE -> getString(R.string.android_app)
                API_TYPE_MAGISK_MODULE -> getString(R.string.magisk_module)
                API_TYPE_SHELL -> getString(R.string.shell)
                API_TYPE_SHELL_ROOT -> getString(R.string.shell_root)
                else -> ""
            }
        }
        val hubUuid = appCloudConfig?.appConfig?.hubInfo?.hubUuid
        val hubName = cloudConfigGetter.getHubCloudConfig(hubUuid)?.info?.hubName
        return CloudConfigListItemView(name, type, hubName, appUuid)
    }

    private fun getCloudHubItemCardView(item: CloudConfig.HubListBean): CloudConfigListItemView {
        val name = item.hubConfigName
        val hubUuid = item.hubConfigUuid
        val hubCloudConfig = cloudConfigGetter.getHubCloudConfig(hubUuid)
        val type = getString(
                if (hubCloudConfig?.applicationsMode?.appUrlTemplate?.isNotEmpty() == true)
                    R.string.applications
                else R.string.app_hub
        )
        return CloudConfigListItemView(name, type, hubUuid, hubUuid)
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "CLOUD_CONFIG_TAG"

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
