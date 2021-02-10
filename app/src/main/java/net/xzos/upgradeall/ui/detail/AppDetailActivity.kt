package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.absinthe.libraries.utils.utils.UiUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.databinding.ActivityAppDetailBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.base.view.ProgressButton
import net.xzos.upgradeall.ui.detail.setting.AppSettingActivity


class AppDetailActivity : AppBarActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var app: App
    private lateinit var viewModel: AppDetailViewModel

    override fun initBinding(): View {
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        binding.appItem = AppDetailViewModel(app).also { viewModel = it }
        binding.handler = AppDetailHandler()
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_app_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_app -> {
                AppSettingActivity.startActivity(this, app)
                true
            }
            R.id.change_hub_priority -> {
                TODO("修改软件源优先级")
            }
            R.id.ignore_current_version -> {
                viewModel.currentVersion?.switchIgnoreStatus()?.also { renewMenu() }
                true
            }
            R.id.ignore_app -> {
                TODO("忽略该跟踪项")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.ignore_current_version)
        val currentVersion = viewModel.currentVersion ?: return false
        item.setTitle(if (currentVersion.isIgnored) R.string.remove_ignore_version
        else R.string.ignore_version)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun getAppBar(): Toolbar = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        bundleApp?.run { app = this } ?: onBackPressed()
        super.onCreate(savedInstanceState)
        getAppBar().showOverflowMenu()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        binding.toolbar.title = binding.appItem?.name ?: ""
    }

    override fun initView() {
        binding.btnUpdate.apply {
            layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                setMargins(marginStart, marginTop, marginEnd, marginBottom + UiUtils.getNavBarHeight(windowManager))
            }
        }
        val versionList = viewModel.versionList
        val items = versionList.map { it.name }
        val adapter = ArrayAdapter(this, R.layout.item_more_version, items)

        binding.tvMoreVersion.run {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                viewModel.setVersionInfo(position)
            }

            // 设置初始的版本信息
            if (!items.isNullOrEmpty()) {
                setText(items[0], false)
            }
            viewModel.setVersionInfo(0)
        }
        binding.btnUpdate.setOnStateListener(object : ProgressButton.OnStateListener {
            override fun onFinish() {
                TODO("Not yet implemented")
            }

            override fun onStop() {
                TODO("Not yet implemented")
            }

            override fun onContinue() {
                TODO("Not yet implemented")
            }

        })
    }

    private fun renewMenu() {
        invalidateOptionsMenu()
    }

    companion object {
        private var bundleApp: App? = null

        fun startActivity(context: Context, app: App) {
            bundleApp = app
            context.startActivity(Intent(context, AppDetailActivity::class.java))
        }
    }
}
