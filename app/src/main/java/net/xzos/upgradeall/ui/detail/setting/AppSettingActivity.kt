package net.xzos.upgradeall.ui.detail.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.get
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.databinding.ActivityAppSettingBinding
import net.xzos.upgradeall.databinding.ItemAppAttrSettingBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.detail.setting.attrlist.AttrListAdapter
import net.xzos.upgradeall.ui.detail.setting.attrlist.AttrListViewModel
import net.xzos.upgradeall.utils.ToastUtil


class AppSettingActivity : AppBarActivity() {

    private val database: AppEntity? = bundleDatabase // 获取可能来自修改设置项的请求
    private lateinit var binding: ActivityAppSettingBinding
    private val viewModel by viewModels<AttrListViewModel>()

    private val attrMap: Map<String, String?>
        get() {
            val attrList = binding.attrList
            val map = mutableMapOf<String, String>()
            for (i in 0 until attrList.size) {
                val view = ItemAppAttrSettingBinding.bind(attrList[i])
                map[view.keyEdit.text.toString()] = view.valueEdit.text.toString()
            }
            return map.filter { it.key.isNotBlank() }.mapValues {
                if (it.value.isBlank())
                    null
                else it.value
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.parse_attr_from_url -> showUrlDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_app_setting, menu)
        return true
    }

    private fun showUrlDialog() {
        UrlParserDialog(fun(map) {
            map.forEach {
                viewModel.addItem(it.key, it.value)
            }
        }).show(supportFragmentManager)
    }

    private suspend fun addApp() {
        val name = binding.nameEdit.text.toString()
        val appId = attrMap
        if (name.isBlank()) {
            binding.nameEdit.error = getString(R.string.helper_text_cant_be_empty)
            return
        }
        if (appId.isEmpty()) {
            ToastUtil.makeText(R.string.helper_text_attr_cant_be_empty)
            return
        }
        val appEntity = database?.apply {
            this.name = name
            this.appId = appId
        } ?: AppEntity(0, name, appId)
        window?.let {
            binding.addButton.visibility = View.GONE
            binding.loadingBar.visibility = View.VISIBLE

            if (AppManager.updateApp(appEntity) != null)
                finish()
            else
                ToastUtil.makeText(R.string.failed_to_add, Toast.LENGTH_LONG)
            binding.addButton.visibility = View.VISIBLE
            binding.loadingBar.visibility = View.GONE
        }
    }

    private fun initSetSettingItem() {
        // 如果是设置修改请求，设置预置设置项
        database?.run {
            binding.nameEdit.setText(name)
            appId.forEach {
                viewModel.addItem(it.key, it.value)
            }
        }
    }

    override fun initBinding(): View {
        binding = ActivityAppSettingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        getAppBar().setTitle(R.string.edit_app)
    }

    override fun initView() {
        binding.addButton.let { fab ->
            fab.setOnClickListener {
                GlobalScope.launch { addApp() }
            }
            fab.visibility = View.VISIBLE
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 刷新第三方源列表，获取支持的第三方源列表
            withContext(Dispatchers.Main) {
                if (HubManager.getHubList().isEmpty()) {
                    ToastUtil.makeText(R.string.add_something, Toast.LENGTH_LONG)
                    finish()
                }
            }
        }

        binding.attrList.adapter = AttrListAdapter(lifecycleScope, viewModel).apply {
            viewModel.adapter = this
        }

        binding.ibAddAttr.setOnClickListener {
            viewModel.addEmptyItem()
        }
        initSetSettingItem() // 设置预置设置项
    }

    companion object {
        private var bundleDatabase: AppEntity? = null

        fun startActivity(context: Context, app: App? = null) {
            bundleDatabase = app?.appDatabase
            context.startActivity(Intent(context, AppSettingActivity::class.java))
        }
    }
}
