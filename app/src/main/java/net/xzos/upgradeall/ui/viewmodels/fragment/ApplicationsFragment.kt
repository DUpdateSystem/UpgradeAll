package net.xzos.upgradeall.ui.viewmodels.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.xzos.dupdatesystem.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.R

/**
 * 应用市场详细数据展示页面
 * 作为框架嵌套到 主页[net.xzos.upgradeall.ui.activity.MainActivity]
 * 由点击 更新项 [net.xzos.upgradeall.ui.viewmodels.adapters.AppItemAdapter] 动作 触发显示
 * 使用 [net.xzos.upgradeall.ui.activity.MainActivity.setFrameLayout] 方法跳转
 */
class ApplicationFragment : Fragment() {
    lateinit var applications: Applications
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bundleApplications?.run {
            applications = this
            initUi()
        } ?: activity?.onBackPressed()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_applications, container, false)
    }

    private fun initUi() {

    }

    companion object {
        // 显示的应用市场对象
        val bundleApplications: Applications? = null
    }
}
