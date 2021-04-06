package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.absinthe.libraries.utils.extensions.addPaddingTop
import com.absinthe.libraries.utils.utils.UiUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.databinding.ActivityAppDetailBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.base.selectlistdialog.SelectItem
import net.xzos.upgradeall.ui.base.selectlistdialog.SelectListDialog
import net.xzos.upgradeall.ui.base.view.ProgressButton
import net.xzos.upgradeall.ui.detail.setting.AppSettingActivity
import net.xzos.upgradeall.utils.actionBarSize


class AppDetailActivity : AppBarActivity() {

    private lateinit var binding: ActivityAppDetailBinding
    private lateinit var app: App
    private val viewModel by viewModels<AppDetailViewModel>()

    override fun initBinding(): View {
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        val item = AppDetailItem(this)
        viewModel.initData(binding, item, app)
        binding.item = item
        binding.handler = AppDetailHandler(viewModel, supportFragmentManager)
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
                val hubList = app.hubListUuid.mapNotNull {
                    HubManager.getHub(it)
                }
                GlobalScope.launch {
                    SelectListDialog.showDialog(hubList.map { SelectItem(it.name, it.uuid, true) }, supportFragmentManager)
                }
                return true
            }
            R.id.ignore_current_version -> {
                viewModel.currentVersion?.switchIgnoreStatus()?.also { renewMenu() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.ignore_current_version)
        val currentVersion = viewModel.currentVersion ?: return true
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
        binding.toolbar.title = binding.item?.appName?.get()
        binding.headerContentLayout.addPaddingTop(actionBarSize())
    }

    override fun initView() {
        binding.btnUpdate.apply {
            layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                setMargins(marginStart, marginTop, marginEnd, marginBottom + UiUtils.getNavBarHeight(windowManager))
            }
        }

        binding.tvMoreVersion.setOnItemClickListener { _, _, position, _ ->
            viewModel.setVersionInfo(position)
        }
        binding.btnUpdate.setOnStateListener(object : ProgressButton.OnStateListener {
            override fun onFinish() {
            }

            override fun onStop() {
            }

            override fun onContinue() {
            }

        })
    }

    private fun renewMenu() {
        invalidateOptionsMenu()
        viewModel.updateData()
    }

    companion object {
        private var bundleApp: App? = null

        fun startActivity(context: Context, app: App) {
            bundleApp = app
            context.startActivity(Intent(context, AppDetailActivity::class.java))
        }
    }
}
