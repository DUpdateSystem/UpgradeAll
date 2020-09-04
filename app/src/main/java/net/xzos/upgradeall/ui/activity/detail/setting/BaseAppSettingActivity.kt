package net.xzos.upgradeall.ui.activity.detail.setting

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_app_setting.*
import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.android.synthetic.main.layout_appbar.*
import kotlinx.android.synthetic.main.list_content.*
import kotlinx.android.synthetic.main.simple_textview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data.database.BaseAppDatabase
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.data.gson.ExtraItem
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.adapters.ExtraAdapter
import net.xzos.upgradeall.utils.IconPalette
import net.xzos.upgradeall.utils.ToastUtil

abstract class BaseAppSettingActivity : AppCompatActivity() {

    internal val database: BaseAppDatabase? = bundleDatabase // 获取可能来自修改设置项的请求

    internal val extraAdapter = ExtraAdapter()

    internal var hubUuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_setting)

        initView()
    }

    private fun setEndHelpIcon() {
        setEndIconOnClickListener(hub_input_layout)
        setEndIconOnClickListener(name_input_layout)
        setEndIconOnClickListener(url_input_layout)
        setEndIconOnClickListener(versioning_input_layout)
    }

    private fun setEndIconOnClickListener(textInputLayout: TextInputLayout) {
        val helpText = getString(when (textInputLayout.id) {
            R.id.hub_input_layout -> R.string.setting_app_hub_explain
            R.id.name_input_layout -> R.string.setting_app_name_explain
            R.id.url_input_layout -> R.string.setting_app_url_explain
            R.id.versioning_input_layout -> R.string.setting_app_versioning_explain
            else -> R.string.null_english
        }) + getString(R.string.detailed_help) + "：" + getString(R.string.readme_url)
        textInputLayout.setEndIconOnClickListener {
            AlertDialog.Builder(it.context).setView(R.layout.simple_textview).create().let { dialog ->
                dialog.show()
                dialog.text.text = helpText
            }
        }
    }

    private fun showHubSelectDialog(vararg editViews: TextInputEditText) {
        BottomSheetDialog(this).apply {
            setContentView(layoutInflater.inflate(R.layout.list_content, viewPager))
            val (hubNameStringList, hubUuidStringList) = renewApiJsonObject()
            list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, hubNameStringList)
            list.setOnItemClickListener { _, _, position, _ ->
                hubUuid = hubUuidStringList[position]
                for (editView in editViews)
                    editView.setText(hubNameStringList[position])
                cancel()
            }
        }.show()
    }

    private fun addApp() {
        if (editHub.text.isNullOrBlank()) {
            hub_input_layout.error = getString(R.string.helper_text_cant_be_empty)
            return
        }
        if (editName.text.isNullOrBlank()) {
            url_input_layout.error = getString(R.string.helper_text_cant_be_empty)
            return
        }

        window?.let {
            floatingActionButton.visibility = View.GONE
            loadingBar.visibility = View.VISIBLE

            if (saveDatabase())
                finish()
            else
                ToastUtil.makeText(R.string.failed_to_add, Toast.LENGTH_LONG)
            floatingActionButton?.visibility = View.VISIBLE
            loadingBar.visibility = View.GONE
        }
    }

    abstract fun saveDatabase(): Boolean

    private fun initSetSettingItem() {
        // 如果是设置修改请求，设置预置设置项
        editName.setText(database?.name)
        hubUuid = database?.hubUuid
        lifecycleScope.launch(Dispatchers.IO) {
            val text = HubDatabaseManager.getDatabase(database?.hubUuid)?.hubConfig?.info?.hubName
            withContext(Dispatchers.Main) {
                editHub.setText(text)
            }
        }
        setSettingItem()
    }

    abstract fun setSettingItem()

    private fun renewApiJsonObject(): Pair<List<String>, List<String>> {
        // api接口名称列表
        // 清空 apiSpinnerList
        val hubNameStringList = mutableListOf<String>()
        val hubUuidStringList = mutableListOf<String>()
        // 获取自定义源
        val hubList = HubDatabaseManager.hubDatabases  // 读取 hub 数据库
        for (hubDatabase in hubList) {
            val name: String = hubDatabase.hubConfig.info.hubName
            val apiUuid: String = hubDatabase.uuid
            hubNameStringList.add(name)
            // 记录可用的api UUID
            hubUuidStringList.add(apiUuid)
        }
        return Pair(hubNameStringList, hubUuidStringList)
    }

    private fun initView() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.taupe)
        toolbar_backdrop_image.setBackgroundColor(IconPalette.getColorInt(R.color.taupe))
        collapsingToolbarLayout.contentScrim = ContextCompat.getDrawable(this, R.color.taupe)
        floatingActionButton.visibility = View.GONE
        floatingActionButton.let { fab ->
            fab.setOnClickListener {
                addApp()
            }
            fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check_mark))
            fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.taupe)))
            fab.setColorFilter(IconPalette.getColorInt(R.color.white))
            fab.visibility = View.VISIBLE
        }
        setEndHelpIcon()
        app_logo_image_view.visibility = View.GONE  // 设置页面难以确定用户希望设置的软件对象，因此隐藏软件图标
        rv_extras.adapter = extraAdapter
        btn_add_extra.setOnClickListener {
            extraAdapter.addData(0, ExtraItem())
        }
        extraAdapter.apply {
            animationEnable = true
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.ib_delete) {
                    removeAt(position)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 刷新第三方源列表，获取支持的第三方源列表
            val (hubNameStringList, _) = renewApiJsonObject()
            // 修改 apiSpinner
            withContext(Dispatchers.Main) {
                if (hubNameStringList.isEmpty()) {
                    ToastUtil.makeText(R.string.add_something, Toast.LENGTH_LONG)
                    MainActivity.setNavigationItemId(R.id.hubCloudFragment)
                    finish()
                }
            }
        }

        initSetSettingItem() // 设置预置设置项
        // 以下是按键事件
        // 判断编辑模式
        if (database is ApplicationsDatabase) {
            // 已隐藏无关选项
            editUrl.visibility = View.GONE
            imageView2.visibility = View.GONE
            textView2.visibility = View.GONE
            versionCheckSpinner.visibility = View.GONE
            versioning_input_layout.visibility = View.GONE
            versionCheckButton.visibility = View.GONE
        }
        val editViews = mutableListOf<TextInputEditText>(editHub).apply {
            if (database is ApplicationsDatabase)
                this.add(editName)
        }.toTypedArray()
        editHub.setOnClickListener {
            showHubSelectDialog(*editViews)
        }
    }

    companion object {
        internal var bundleDatabase: BaseAppDatabase? = null
            get() {
                val app = field
                field = null
                return app
            }
    }
}
