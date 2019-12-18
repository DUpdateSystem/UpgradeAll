package net.xzos.upgradeAll.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.devs.vectorchildfinder.VectorChildFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.data.database.manager.AppDatabaseManager
import net.xzos.upgradeAll.server.ServerContainer


object IconPalette {

    val fabAddIcon = getPlus(getColorInt(R.color.light_gray))

    val fabDownloadIcon = getDownload(getColorInt(R.color.white))

    val appItemPlaceholder = getCheckMark(
            getColorInt(R.color.white),
            getColorInt(R.color.light_gray)
    )

    val editIcon = getEdit(getColorInt(R.color.text_lowest_priority_color))

    @Suppress("DEPRECATION")
    fun getColorInt(colorRes: Int) = run {
        val context = MyApplication.context
        return@run if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            context.getColor(colorRes)
        else
            context.resources.getColor(colorRes)

    }

    private fun getPlus(bodyColor: Int) =
            changeDrawableColor(bodyColor, null, R.drawable.ic_plus)

    private fun getDownload(bodyColor: Int) =
            changeDrawableColor(bodyColor, null, R.drawable.ic_download)

    private fun getCheckMark(bodyColor: Int, backgroundColor: Int) =
            changeDrawableColor(bodyColor, backgroundColor, R.drawable.ic_check_mark_circle)

    private fun getEdit(bodyColor: Int) =
            changeDrawableColor(bodyColor, null, R.drawable.ic_edit)

    private fun changeDrawableColor(bodyColor: Int?, backgroundColor: Int?, drawableId: Int): Drawable {
        val context = MyApplication.context
        return ImageView(context).also { iv ->
            VectorChildFinder(context, drawableId, iv).also { vector ->
                if (bodyColor != null) vector.findPathByName("body").also {
                    it.fillColor = bodyColor
                }
                if (backgroundColor != null) vector.findPathByName("background").also {
                    it.fillColor = backgroundColor
                }
            }
        }.drawable
    }

    fun loadHubIconView(iconImageView: ImageView, hubIconUrl: String?) =
            loadIconView(iconImageView,
                    defaultSrc = MyApplication.context.getDrawable(R.drawable.ic_android_placeholder),
                    iconInfo = Pair(hubIconUrl, null)
            )

    fun loadAppIconView(iconImageView: ImageView, appDatabaseId: Long = 0, iconInfo: Pair<String?, String?>? = null) =
            loadIconView(iconImageView,
                    defaultSrc = MyApplication.context.getDrawable(R.drawable.ic_android_placeholder),
                    appDatabaseId = appDatabaseId,
                    iconInfo = iconInfo
            )

    private fun loadIconView(iconImageView: ImageView, defaultSrc: Drawable? = null, appDatabaseId: Long = 0, iconInfo: Pair<String?, String?>? = null) {
        GlobalScope.launch {
            val appDatabase = AppDatabaseManager.getDatabase(appDatabaseId)
            val (appIconUrl, appModuleName) = iconInfo ?: Pair(
                    ServerContainer.AppManager.getApp(appDatabaseId).engine.getAppIconUrl()
                    , appDatabase?.targetChecker?.extraString
            )
            launch(Dispatchers.Main) {
                val activity = getActivity(iconImageView)
                if (activity?.isFinishing != true) {
                    iconImageView.visibility = View.GONE
                    try {
                        Glide.with(iconImageView).load(appIconUrl ?: "").let {
                            if (appIconUrl == null) {
                                it.placeholder(
                                        try {
                                            iconImageView.context.packageManager.getApplicationIcon(appModuleName!!)
                                        } catch (e: Throwable) {
                                            defaultSrc ?: return@let
                                        }
                                )
                            }
                            it.into(iconImageView)
                            iconImageView.visibility = View.VISIBLE
                        }
                    } catch (e: IllegalArgumentException) {
                    }
                }
            }
        }
    }

    private fun getActivity(view: View): Activity? {
        var context: Context = view.context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
}