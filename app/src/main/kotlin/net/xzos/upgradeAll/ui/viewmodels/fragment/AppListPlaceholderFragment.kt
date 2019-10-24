package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.pageview_app_list.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.viewmodel.AppListPageViewModel

class AppListPlaceholderFragment : Fragment() {

    private lateinit var mContext: Context
    private lateinit var appListPageViewModel: AppListPageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context!!
        appListPageViewModel = ViewModelProvider(this).get(AppListPageViewModel::class.java)
        val hubUuid = arguments?.getString(ARG_SECTION_NUMBER)
        if (hubUuid != null)
            appListPageViewModel.setHubUuid(hubUuid)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.pageview_app_list, container, false)
        root.findViewById<TextView>(R.id.updateOverviewNumberTextView)?.let {
            appListPageViewModel.needUpdateAppIdLiveLiveData.observe(this,
                    Observer<MutableList<Long>> { list ->
                        it.text = list.size.toString()
                    })
        }
        root.findViewById<TextView>(R.id.updateOverviewTextView)?.let {
            val updateOverviewStringList = it.context.getString(R.string.example_update_overview).split("0")
                    .filter { element -> element.isNotBlank() }
            var appListNum = 0
            var needUpdateAppNum = 0
            appListPageViewModel.appCardViewList.observe(this, Observer { list ->
                with(list.size) {
                    appListNum = if (this > 0)
                        this - 1
                    else
                        this
                }
                it.text = "$appListNum${updateOverviewStringList[0]}$needUpdateAppNum${updateOverviewStringList[1]}"
            })
            appListPageViewModel.needUpdateAppIdLiveLiveData.observe(this,
                    Observer<MutableList<Long>> { num ->
                        needUpdateAppNum = num.size
                        it.text = "$appListNum${updateOverviewStringList[0]}$needUpdateAppNum${updateOverviewStringList[1]}"
                    })
        }

        val layoutManager = GridLayoutManager(activity, 1)
        val cardItemRecyclerView = root.findViewById<RecyclerView>(R.id.cardItemRecyclerView)
        cardItemRecyclerView.layoutManager = layoutManager
        val adapter = AppItemAdapter(appListPageViewModel.needUpdateAppIdLiveLiveData, appListPageViewModel.appCardViewList, this)
        cardItemRecyclerView.adapter = adapter
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        //swipeRefresh.setOnRefreshListener { this.renewCardView() }
    }

    override fun onResume() {
        super.onResume()
        appListPageViewModel.appCardViewList.value?.let {
            if (it.isNullOrEmpty()) {
                updateOverviewLayout.visibility = View.GONE
                placeholderLayout.visibility = View.VISIBLE
                placeholderImageVew.setImageResource(R.drawable.ic_isnothing_placeholder)
                with(placeholderTextView) {
                    text = this.context.getText(R.string.click_to_add_something)
                }
            } else {
                updateOverviewLayout.visibility = View.VISIBLE
                placeholderLayout.visibility = View.GONE
            }
        }
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "hubUuidTag"

        internal fun newInstance(hubUuid: String): AppListPlaceholderFragment {
            val fragment = AppListPlaceholderFragment()
            val bundle = Bundle()
            bundle.putString(ARG_SECTION_NUMBER, hubUuid)
            fragment.arguments = bundle
            return fragment
        }
    }
}