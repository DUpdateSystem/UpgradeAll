package net.xzos.upgradeAll.ui.activity

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_hub_debug.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.json.gson.HubConfig
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.app.engine.js.utils.JSLog
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.server.log.LogDataProxy
import net.xzos.upgradeAll.utils.AriaDownloader
import net.xzos.upgradeAll.utils.FileUtil
import org.apache.commons.text.StringEscapeUtils
import java.io.File

class HubDebugActivity : AppCompatActivity() {

    private var hubConfigUri: Uri? = null
    private var jsUri: Uri? = null

    private var jsCardViewWrapHeight = 0
    private var configCardViewWrapHeight = 0

    private var screenHeight = 0

    private lateinit var selectedFileAddress: String

    private val hubConfigGson: HubConfig?
        get() {
            val hubConfig = HubConfig()
            try {
                hubConfig.baseVersion = Integer.parseInt(configBaseVersionEditText.text.toString())
            } catch (e: NumberFormatException) {
                Toast.makeText(this@HubDebugActivity, "请填写正确整数", Toast.LENGTH_LONG).show()
                return null
            }

            hubConfig.uuid = configUuidEditText.text.toString()
            hubConfig.info = HubConfig.InfoBean()
            hubConfig.info.hubName = configHubNameEditText.text.toString()
            try {
                hubConfig.info.configVersion = Integer.parseInt(configVersionEditText.text.toString())
            } catch (e: NumberFormatException) {
                Toast.makeText(this@HubDebugActivity, "请填写正确整数", Toast.LENGTH_LONG).show()
                return null
            }

            hubConfig.webCrawler = HubConfig.WebCrawlerBean()
            hubConfig.webCrawler.tool = configToolEditText.text.toString()
            hubConfig.webCrawler.filePath = jsFilePathEditText.text.toString()
            return hubConfig
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hub_debug)
        // toolbar 点击事件
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val intent = intent
        val uuid = intent.getStringExtra("hub_uuid")

        // 读取 JS 脚本
        selectJsFileButton.setOnClickListener { FileUtil.performFileSearch(this, READ_JS_REQUEST_CODE, "application/*") }

        // 读取配置文件
        selectConfigFileButton.setOnClickListener { FileUtil.performFileSearch(this, READ_CONFIG_REQUEST_CODE, "application/*") }
        selectConfigFileButton.setOnLongClickListener {
            var configName = configHubNameEditText.text.toString()
            if (configName != "") {
                configName += ".json"
            } else
                configName = "config.json"
            FileUtil.createFile(this, WRITE_CONFIG_REQUEST_CODE, "application/*", configName)
            true
        }

        // 运行 JS 脚本
        jsRunButton.setOnClickListener {
            runTestJs()
            jsRunButton.isClickable = false
            Thread.sleep(AriaDownloader.blockingTime)
            jsRunButton.isClickable = true
        }

        jsFilePathEditTextPerformLongClick()

        // 保存配置
        saveButton.setOnClickListener { v ->
            val popupMenu = PopupMenu(this, v)
            val menuInflater = popupMenu.menuInflater
            menuInflater.inflate(R.menu.menu_save_config, popupMenu.menu)
            popupMenu.show()
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.saveToFileButton -> writeHubConfigToFile()
                    R.id.saveToDatabaseButton -> addHubConfigToDatabase()
                    R.id.saveToFileAndDatabaseButton -> {
                        addHubConfigToDatabase()
                        writeHubConfigToFile()
                    }
                }
                true
            }
        }
        // 折叠展开卡片
        jsShrinkLayout.setOnClickListener {
            cardViewAnim(jsTextView, jsTestCardView, jsContentLayout)
        }
        configShrinkLayout.setOnClickListener {
            cardViewAnim(configTextView, hubConfigCardView, configContentLayout)
        }

        if (uuid != null) loadFromDatabase(uuid)

        setUI()

        fixScrollTouch()

        FileUtil.requestPermission(this, PERMISSIONS_REQUEST_READ_CONTACTS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            val uri = resultData.data
            if (uri != null)
                when (requestCode) {
                    READ_JS_REQUEST_CODE -> loadJSFromUri(uri)
                    READ_CONFIG_REQUEST_CODE -> loadConfigFromUri(uri)
                    READ_CONFIG_JS_REQUEST_CODE -> loadConfigJSFormUri(uri)
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "编辑测试生成配置需要读写本地文件", Toast.LENGTH_LONG).show()
                onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        LogDataProxy(Log).clearLogSort("DeBug")
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.app_help -> {
                var intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://xzos.net/the-customizing-configuration-rules-for-a-software-depot/")
                intent = Intent.createChooser(intent, "请选择浏览器以查看帮助文档")
                startActivity(intent)
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 修复滑动动作无法传递
    @SuppressLint("ClickableViewAccessibility")
    private fun fixScrollTouch() {
        scrollView.setOnTouchListener { _, _ ->
            jsTestTextView.parent.requestDisallowInterceptTouchEvent(false)
            jsLogTextView.parent.requestDisallowInterceptTouchEvent(false)
            false
        }

        jsTestTextView.setOnTouchListener { _, _ ->
            jsTestTextView.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        jsLogTextView.setOnTouchListener { _, _ ->
            jsLogTextView.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

    private fun setUI() {
        // 限制 JS 代码展示区域
        val displayMetrics = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenHeight = displayMetrics.heightPixels
        jsTestTextView.maxHeight = screenHeight / 3
        jsTestTextView.movementMethod = ScrollingMovementMethod.getInstance()

        // 限制 Log 展示区域
        jsLogTextView.maxHeight = screenHeight * 5 / 12
        jsLogTextView.movementMethod = ScrollingMovementMethod.getInstance()

        // 初始设置文件选择按钮提示信息
        selectedFileAddress = getString(R.string.selected_file_address)
        val nullSelectedFileAddress = selectedFileAddress + "NULL"

        selectJsFileButton.text = nullSelectedFileAddress
        selectConfigFileButton.text = nullSelectedFileAddress

        // 折叠 JS 测试卡片
        jsTestCardView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                jsTestCardView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                jsCardViewWrapHeight = jsTestCardView.height
                // 折叠 JS 测试区
                cardViewAnim(jsTextView, jsTestCardView, jsContentLayout)
            }
        })
        hubConfigCardView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                hubConfigCardView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                configCardViewWrapHeight = hubConfigCardView.height
            }
        })
    }

    // 自定义 jsFilePathEditText 长按菜单
    private fun jsFilePathEditTextPerformLongClick() {
        jsFilePathEditText.setOnLongClickListener {
            FileUtil.performFileSearch(this, READ_CONFIG_JS_REQUEST_CODE, "application/*")
            Toast.makeText(this, "如果你需要编辑文字，可以双击编辑框而非长按", Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }
        jsFilePathEditText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val menuInflater = mode.menuInflater
                menuInflater.inflate(R.menu.menu_long_click_js_edit, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (item.itemId == R.id.testJS) {
                    val jsRelativePath = jsFilePathEditText.text.toString()
                    var configPath = FileUtil.uriToPath(hubConfigUri!!)
                    if (hubConfigUri != null && jsRelativePath != "") {
                        configPath = configPath.substring(0, configPath.lastIndexOf("/"))
                        val jsPath = FileUtil.pathTransformRelativeToAbsolute(configPath, jsRelativePath)
                        if (FileUtil.fileIsExistsByPath(jsPath)) {
                            loadJSFromUri(Uri.fromFile(File(jsPath)))
                            Toast.makeText(this@HubDebugActivity, "现在打开第一个卡片进行 JS 脚本测试", Toast.LENGTH_LONG).show()
                        } else
                            Toast.makeText(this@HubDebugActivity, "文件不存在", Toast.LENGTH_LONG).show()
                    } else
                        Toast.makeText(this@HubDebugActivity, "请先选择一个正确的 JS脚本", Toast.LENGTH_LONG).show()
                    mode.finish()
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        }
    }

    private fun runTestJs() {
        if (jsUri != null) {
            loadJSFromUri(jsUri)
        }
        val jsCode = jsTestTextView.text.toString()
        if (jsCode == "") {
            Toast.makeText(this@HubDebugActivity, "请选择一个正确的 JS 脚本文件", Toast.LENGTH_LONG).show()
            return
        }
        val testUrl = testUrlEditText.text.toString()
        if (testUrl == "") {
            Toast.makeText(this@HubDebugActivity, "请填写 测试网址", Toast.LENGTH_LONG).show()
            return
        }
        val logObjectTag = arrayOf("DeBug", "0")
        val jsLog = JSLog(logObjectTag)  // 连接日志系统以打印提示信息
        jsLog.d(" \n----------------Start----------------")
        val logListLiveData = LogDataProxy(Log).getLogMessageListLiveData(logObjectTag)
        logListLiveData.observe(this, Observer { logList ->
            val textViewMessage = StringBuilder()
            for (logMessage in logList)
                textViewMessage.append(StringEscapeUtils.unescapeJava(logMessage)).append("\n")
            jsLogTextView.text = textViewMessage.toString()
        })
        jsLogTextView.visibility = View.VISIBLE
        GlobalScope.launch {
            // 初始化 JS 组件
            val javaScriptEngine = JavaScriptEngine(logObjectTag, testUrl, jsCode, isDebug = true)  // 创建 JS 引擎
            // 分步测试
            jsLog.d("1. 获取默认名称(defaultName): ${javaScriptEngine.getDefaultName()} \n")
            jsLog.d("2. 获取发布版本号总数(releaseNum): ${javaScriptEngine.getReleaseNum()} \n")
            for (i in 0 until javaScriptEngine.getReleaseNum()) {
                jsLog.d("3. ($i) 获取发布版本号(getVersioning): ${javaScriptEngine.getVersioning(i)} \n")
                jsLog.d("4. ($i) 获取发布版本号(getChangelog): ${javaScriptEngine.getChangelog(i)} \n")
            }
            for (i in 0 until javaScriptEngine.getReleaseNum()) {
                val releaseDownload = javaScriptEngine.getReleaseDownload(i)
                jsLog.d("5. ($i) 获取下载链接(getReleaseDownload): $releaseDownload \n")
            }
            val releaseDownloadFilePath = javaScriptEngine.downloadReleaseFile(Pair(0, 0))
            jsLog.d("5. 尝试下载 最新版本的第一个文件(downloadReleaseFile): ${releaseDownloadFilePath != null} \n    下载文件地址: $releaseDownloadFilePath \n")
            if (releaseDownloadFilePath != null) File(releaseDownloadFilePath).deleteOnExit()
            jsLog.d(" \n----------------End----------------")
        }
    }

    private fun cardViewAnim(titleTextView: TextView, cardView: CardView, contentLayout: RelativeLayout) {
        val layoutParams = cardView.layoutParams as LinearLayout.LayoutParams
        val animStatus: Boolean // true 展开，false 折叠
        val cardViewHeight = cardView.height
        val cardViewFoldHeight = screenHeight / 15
        val animTime = 1000
        var cardViewWrapHeight = 0
        if (cardView === jsTestCardView)
            cardViewWrapHeight = jsCardViewWrapHeight
        else if (cardView === hubConfigCardView)
            cardViewWrapHeight = configCardViewWrapHeight
        // 获取 wrap 高度
        if (layoutParams.height == WindowManager.LayoutParams.WRAP_CONTENT)
            cardViewWrapHeight = cardViewHeight
        var start = cardViewHeight
        var end: Int
        if (titleTextView.visibility != View.VISIBLE) {
            // 折叠
            animStatus = false
            end = cardViewFoldHeight
            if (end > start) {
                end = start
                start = end
            }
        } else {
            // 展开
            animStatus = true
            end = cardViewWrapHeight
            if (end < start) {
                end = start
                start = end
            }
            // 为了美观，延迟卡片信息更新至最后
        }
        // 启动动画
        val valueAnimator = ValueAnimator.ofInt(start, end)
        valueAnimator.duration = animTime.toLong()
        valueAnimator.addUpdateListener { animator ->
            layoutParams.height = animator.animatedValue as Int
            cardView.layoutParams = layoutParams
        }
        valueAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (!animStatus) {
                    // 折叠
                    titleTextView.visibility = View.VISIBLE
                    contentLayout.visibility = View.INVISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                if (animStatus) {
                    // 展开
                    contentLayout.visibility = View.VISIBLE
                    titleTextView.visibility = View.INVISIBLE
                    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                    cardView.layoutParams = layoutParams
                }
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
        valueAnimator.start()
        // 设置点击事件
        if (animStatus)
            cardView.setOnClickListener(null)
        else
            cardView.setOnClickListener { cardViewAnim(titleTextView, cardView, contentLayout) }
        // 保存数据
        jsCardViewWrapHeight = cardViewWrapHeight
        configCardViewWrapHeight = cardViewWrapHeight
    }

    private fun loadFromDatabase(uuid: String) {
        val jsCode = HubManager.getJsCode(uuid)
        jsTestTextView.text = jsCode
        val hubConfig = HubManager.getDatabase(uuid)?.hubConfig
        if (hubConfig is HubConfig)
            loadConfigFromHubConfig(hubConfig)
    }

    private fun loadJSFromUri(uri: Uri?) {
        if (uri == null) return
        val jsCode = FileUtil.readTextFromUri(uri) ?: return
        jsUri = uri
        // 更新按钮提示信息
        val path = FileUtil.uriToPath(uri)
        val selectedJsFileText = selectedFileAddress + path
        selectJsFileButton.text = selectedJsFileText
        jsTestTextView.text = jsCode
    }

    private fun loadConfigJSFormUri(uri: Uri) {
        if (hubConfigUri != null) {
            val configPath = FileUtil.uriToPath(hubConfigUri!!)
            val jsAbsolutePath = FileUtil.uriToPath(uri)
            val jsPath = FileUtil.pathTransformAbsoluteToRelative(configPath, jsAbsolutePath)
            jsFilePathEditText.setText(jsPath)
        }
    }

    private fun loadConfigFromUri(uri: Uri) {
        val configString = FileUtil.readTextFromUri(uri)
        hubConfigUri = uri
        // 更新按钮提示信息
        val path = FileUtil.uriToPath(uri)
        val selectedJsFileText = selectedFileAddress + path
        selectConfigFileButton.text = selectedJsFileText
        val hubConfig: HubConfig
        try {
            hubConfig = Gson().fromJson(configString, HubConfig::class.java)
        } catch (e: RuntimeException) {
            Toast.makeText(this@HubDebugActivity, "请选择一个正确的配置文件", Toast.LENGTH_LONG).show()
            return
        }

        loadConfigFromHubConfig(hubConfig)
    }


    private fun loadConfigFromHubConfig(hubConfig: HubConfig?) {
        // 载入数据
        if (hubConfig != null) {
            configBaseVersionEditText.setText(hubConfig.baseVersion.toString())
            configUuidEditText.setText(hubConfig.uuid)
            configHubNameEditText.setText(hubConfig.info.hubName)
            configVersionEditText.setText(hubConfig.info.configVersion.toString())
            configToolEditText.setText(hubConfig.webCrawler.tool)
            jsFilePathEditText.setText(hubConfig.webCrawler.filePath)
        }
    }

    private fun addHubConfigToDatabase() {
        // 获取数据
        val hubConfigGson = hubConfigGson
        // 存入数据
        if (hubConfigGson != null) {
            loadJSFromUri(jsUri)
            val jsCode = jsTestTextView.text.toString()
            if (jsCode != "") {
                val addHubSuccess = HubManager.add(hubConfigGson, jsCode)
                if (addHubSuccess) {
                    Toast.makeText(this@HubDebugActivity, "数据库添加成功", Toast.LENGTH_LONG).show()
                } else
                    Toast.makeText(this@HubDebugActivity, "什么？数据库添加失败！", Toast.LENGTH_LONG).show()
            } else
                Toast.makeText(this@HubDebugActivity, "JS 代码为空", Toast.LENGTH_LONG).show()
        }
    }

    private fun writeHubConfigToFile() {
        // 获取数据
        val hubConfigGson = hubConfigGson
        val gson = GsonBuilder().setPrettyPrinting().create()
        val gsonText = gson.toJson(hubConfigGson)
        // 存入数据
        if (hubConfigUri == null)
            Toast.makeText(this@HubDebugActivity, "请选择配置文件，若无配置文件，你可以长按文件选择框创建新文件", Toast.LENGTH_LONG).show()
        else if (hubConfigUri?.path != null && hubConfigGson != null) {
            val writeSuccess: Boolean = FileUtil.writeTextFromUri(hubConfigUri!!, gsonText)
            if (writeSuccess) {
                Toast.makeText(this@HubDebugActivity, "文件保存成功", Toast.LENGTH_LONG).show()
            } else
                Toast.makeText(this@HubDebugActivity, "什么？文件保存失败！", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private val Log = ServerContainer.Log

        private const val PERMISSIONS_REQUEST_READ_CONTACTS = 1

        private const val READ_JS_REQUEST_CODE = 2
        private const val READ_CONFIG_REQUEST_CODE = 3
        private const val WRITE_CONFIG_REQUEST_CODE = READ_CONFIG_REQUEST_CODE
        private const val READ_CONFIG_JS_REQUEST_CODE = 4
    }
}