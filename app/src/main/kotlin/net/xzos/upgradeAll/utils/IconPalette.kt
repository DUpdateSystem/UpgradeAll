package net.xzos.upgradeAll.utils

import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import com.devs.vectorchildfinder.VectorChildFinder
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication


object IconPalette {

    val fabAddIcon = getPlus(getColorInt(R.color.light_gray))

    val fabDownloadIcon = getDownload(getColorInt(R.color.white))

    val appItemPlaceholder = getCheckMark(
            getColorInt(R.color.white),
            getColorInt(R.color.light_gray)
    )

    val editIcon = getEdit(getColorInt(R.color.text_lowest_priority_color))

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
            changeDrawableColor(bodyColor, backgroundColor, R.drawable.ic_check_mark)

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
}