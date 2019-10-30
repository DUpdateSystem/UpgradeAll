package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.fragment_app_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer.Companion.AppManager
import net.xzos.upgradeAll.server.app.manager.module.Updater
import org.litepal.LitePal
import org.litepal.extension.find

/**
 * 更新项详细数据展示页面
 * 作为框架嵌套到 主页[net.xzos.upgradeAll.ui.activity.MainActivity]
 * 由点击 更新项 [net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter] 动作 触发显示
 * 使用 [net.xzos.upgradeAll.ui.activity.MainActivity.setFrameLayout] 方法跳转
 */
class AppInfoFragment : Fragment() {
    private var appDatabaseId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).let {
            //it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            //it.findViewById<DrawerLayout>(R.id.drawerLayout)?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            activity?.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.let { fab ->
                fab.visibility = View.VISIBLE
                fab.setImageResource(R.drawable.ic_plus)
            }
        }
        arguments?.let {
            appDatabaseId = it.getLong(APP_DATABASE_ID)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as AppCompatActivity).let {
            //it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            //it.findViewById<DrawerLayout>(R.id.drawerLayout)?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            activity?.findViewById<FloatingActionButton>(R.id.floatingActionButton)?.let { fab ->
                fab.setOnClickListener(null)
                fab.visibility = View.GONE
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appDatabase: RepoDatabase? = LitePal.find(appDatabaseId)
        if (appDatabase != null)
            GlobalScope.launch {
                val app = AppManager.getApp(appDatabaseId)
                val updater = Updater(app.engine)
                val installedVersioning = app.installedVersioning
                val latestVersioning = updater.getLatestVersioning()
                val latestChangeLog = updater.getLatestChangelog()
                runBlocking(Dispatchers.Main) {
                    loadAppIconView(appDatabase, appIconImageView)
                    nameTextView.text = appDatabase.name
                    appModuleName.text = appDatabase.versionCheckerGson?.text ?: ""
                    versioningTextView.text = installedVersioning ?: ""
                    localVersioningTextView.text = installedVersioning
                            ?: getString(R.string.null_english)
                    appUrlTextView.text = appDatabase.url
                    cloudVersioningTextView.text = latestVersioning
                    appChangelogTextView.text = latestChangeLog
                            ?: getString(R.string.null_english)
                }
            }
    }

    private fun loadAppIconView(appDatabase: RepoDatabase, imageView: ImageView) {
        GlobalScope.launch {
            val (appIconUrl, appModuleName) = Pair(
                    runBlocking { AppManager.getApp(appDatabaseId).engine.getAppIconUrl() }
                    , appDatabase.versionCheckerGson?.text
            )
            launch(Dispatchers.Main) {
                if (appIconUrl != null) {
                    Glide.with(imageView)
                            .load(appIconUrl)
                            .into(imageView)
                } else if (appModuleName != null) {
                    val packageManager = imageView.context.packageManager
                    try {
                        packageManager.getPackageInfo(appModuleName, 0)
                        val icon = packageManager.getApplicationIcon(appModuleName)
                        Glide.with(imageView)
                                .load("")
                                .placeholder(icon)
                                .into(imageView)
                    } catch (e: PackageManager.NameNotFoundException) {
                    }
                }
            }
        }
    }

    companion object {
        internal const val APP_DATABASE_ID = "app_database_id"
    }
}
