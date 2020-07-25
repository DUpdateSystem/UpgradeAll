package net.xzos.upgradeall.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.pageview_app_list.*
import kotlinx.android.synthetic.main.pageview_app_list.view.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListContainerViewModel

abstract class AppListContainerFragment : Fragment() {
    internal lateinit var viewModel: AppListContainerViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.pageview_app_list, container, false).apply {
            viewModel.appCardViewList.observe(viewLifecycleOwner, Observer {
                placeholderLayout.visibility = if (it.isEmpty())
                    View.VISIBLE
                else View.GONE
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPlaceholderLayoutObserve()
        setNeedUpdateNumNotification()
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener { renewCardView() }
    }

    private fun initPlaceholderLayoutObserve() {
        setPlaceholderLayoutVisibility(View.VISIBLE)
        placeholderImageVew.setImageResource(R.drawable.ic_isnothing_placeholder)
        placeholderTextView.setText(R.string.click_to_add_something)
        viewModel.appCardViewList.observe(viewLifecycleOwner, Observer {
            // 列表显示刷新
            setPlaceholderLayoutVisibility(
                    if (it.isNullOrEmpty())
                        View.VISIBLE
                    else View.GONE
            )
        })
    }

    private fun setPlaceholderLayoutVisibility(visibility: Int) {
        when (visibility) {
            View.VISIBLE -> {
                updateOverviewLayout.visibility = View.GONE
                placeholderLayout.visibility = View.VISIBLE
            }
            else -> {
                updateOverviewLayout.visibility = View.VISIBLE
                placeholderLayout.visibility = View.GONE
            }
        }
    }

    private fun setNeedUpdateNumNotification() {
        var appListNum = 0
        var needUpdateAppNum = 0
        viewModel.appCardViewList.observe(viewLifecycleOwner, Observer { list ->
            with(list.size - 1) {
                appListNum = if (this >= 0) this else this
            }
            updateOverviewTextView.text = getString(R.string.example_update_overview, appListNum, needUpdateAppNum)
        })
        viewModel.needUpdateAppsLiveData.observe(viewLifecycleOwner, Observer { list ->
            needUpdateAppNum = list.size
            updateOverviewTextView.text = getString(R.string.example_update_overview, appListNum, needUpdateAppNum)
            if (needUpdateAppNum == 0) {
                updateOverviewStatusImageView.setImageResource(R.drawable.ic_check_mark)
                updateOverviewNumberTextView.visibility = View.GONE
            } else {
                updateOverviewStatusImageView.setImageResource(R.drawable.ic_up)
                updateOverviewNumberTextView.visibility = View.VISIBLE
                updateOverviewNumberTextView.text = needUpdateAppNum.toString()
            }
        })
    }

    private fun renewCardView() {
        swipeRefreshLayout?.isRefreshing = true
        renewAppList()
        swipeRefreshLayout?.isRefreshing = false
    }

    abstract fun renewAppList()
}
