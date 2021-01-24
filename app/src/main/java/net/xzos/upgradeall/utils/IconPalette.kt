package net.xzos.upgradeall.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.devs.vectorchildfinder.VectorChildFinder
import kotlinx.coroutines.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.getPackageId
import java.io.File
import java.util.concurrent.Executors


object IconPalette {
    private val coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val context = MyApplication.context

    val fabAddIcon = getPlus(getColorInt(R.color.light_gray))
    val fabUpdateIcon = getUpdate(getColorInt(R.color.light_gray))

    val fabDownloadIcon = getDownload(getColorInt(R.color.white))

    val appItemPlaceholder = getCheckMark(
            getColorInt(R.color.white),
            getColorInt(R.color.light_gray)
    )

    val editIcon = getEdit(getColorInt(R.color.text_lowest_priority_color))

    @Suppress("DEPRECATION")
    fun getColorInt(colorRes: Int) = run {
        return@run if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            context.getColor(colorRes)
        else
            context.resources.getColor(colorRes)

    }

    private fun getPlus(bodyColor: Int) =
            changeDrawableColor(bodyColor, null, R.drawable.ic_plus)

    private fun getUpdate(bodyColor: Int) =
            changeDrawableColor(bodyColor, null, R.drawable.ic_system_update)

    private fun getDownload(bodyColor: Int) =
            changeDrawableColor(bodyColor, null, R.drawable.ic_download)

    private fun getCheckMark(bodyColor: Int, backgroundColor: Int) =
            changeDrawableColor(bodyColor, backgroundColor, R.drawable.ic_check_mark_circle)

    private fun getEdit(bodyColor: Int) =
            changeDrawableColor(bodyColor, null, R.drawable.ic_edit)

    private fun changeDrawableColor(bodyColor: Int?, backgroundColor: Int?, drawableId: Int): Drawable {
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

    fun loadHubIconView(
            iconImageView: ImageView,
            hubIconUrl: String? = null,
            file: File? = null,
            hubIconDrawableId: Int? = null
    ) {
        GlobalScope.launch(coroutineDispatcher) {
            loadIconView(iconImageView,
                    IconInfo(url = hubIconUrl,
                            drawable = ContextCompat.getDrawable(
                                    context, hubIconDrawableId ?: R.drawable.ic_application),
                            file = file
                    )
            )
        }
    }

    fun loadApplicationsIconView(
            iconImageView: ImageView,
            hubIconUrl: String? = null
    ) {
        GlobalScope.launch(coroutineDispatcher) {
            loadIconView(iconImageView, IconInfo(url = hubIconUrl,
                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_application)
            ))
        }
    }

    fun loadAppIconView(
            iconImageView: ImageView,
            iconInfo: IconInfo? = null,
            app: App? = null
    ) {
        GlobalScope.launch(coroutineDispatcher) {
            loadIconView(iconImageView,
                    (iconInfo ?: IconInfo(
                            app_package = app?.appId?.getPackageId()?.second
                    )).also {
                        it.drawable = ContextCompat.getDrawable(context, R.drawable.ic_android_placeholder)
                    }
            )
        }
    }

    private suspend fun loadIconView(iconImageView: ImageView, iconInfo: IconInfo) {
        val (url, drawable, appModuleName, file) = iconInfo
        val activity = getActivity(iconImageView)
        if (activity?.isFinishing != true) {
            val model = if (file?.exists() == true) file
            else url ?: try {
                if (appModuleName != null)
                    iconImageView.context.packageManager.getApplicationIcon(appModuleName)
                else null
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } ?: drawable
            if (model != null) {
                val viewTarget = Glide.with(iconImageView).load(model)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                withContext(Dispatchers.Main) {
                    viewTarget.into(iconImageView)
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

data class IconInfo(
        // appIconInfo: Pair<Url, moduleName>
        // cloudHubIconInfo: Pair<hubConfigUrl(configFileName), null>
        val url: String? = null,
        var drawable: Drawable? = null,
        val app_package: String? = null,
        val file: File? = null
)
