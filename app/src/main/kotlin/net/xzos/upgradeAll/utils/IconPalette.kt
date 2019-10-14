package net.xzos.upgradeAll.utils

import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import com.devs.vectorchildfinder.VectorChildFinder
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication


object IconPalette {

    val AppItemPlaceholder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val context = MyApplication.context
        getCheckMark(
                context.getColor(R.color.white),
                context.getColor(R.color.light_gray)
        )
    } else {
        val context = MyApplication.context
        @Suppress("DEPRECATION")
        getCheckMark(
                context.resources.getColor(R.color.white),
                context.resources.getColor(R.color.light_gray)
        )
    }

    private fun getCheckMark(bodyColor: Int, backgroundColor: Int): Drawable {
        return changeDrawableColor(bodyColor, backgroundColor, R.drawable.ic_check_mark)
    }

    private fun changeDrawableColor(bodyColor: Int, backgroundColor: Int, drawableId: Int): Drawable {
        val context = MyApplication.context
        return ImageView(context).also { iv ->
            VectorChildFinder(context, drawableId, iv).also { vector ->
                vector.findPathByName("body").also {
                    it.fillColor = bodyColor
                }
                vector.findPathByName("background").also {
                    it.fillColor = backgroundColor
                }
            }
        }.drawable
    }
}