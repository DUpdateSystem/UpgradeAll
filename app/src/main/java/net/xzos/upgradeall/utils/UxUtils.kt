package net.xzos.upgradeall.utils

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.View
import com.absinthe.libraries.utils.utils.UiUtils
import net.xzos.upgradeall.core.utils.getAppName
import net.xzos.upgradeall.utils.egg.setAppEggTitleSuffix
import java.util.regex.Pattern

object UxUtils {
    fun getStatusBarHeight(resources: Resources): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    fun getRandomColor(): Int {
        val range = if (UiUtils.isDarkMode()) {
            (68..136)
        } else {
            (132..200)
        }
        val r = range.random()
        val g = range.random()
        val b = range.random()

        return Color.parseColor(String.format("#%02x%02x%02x", r, g, b))
    }

    fun getFirstChar(str: String, upperCase: Boolean): String {
        val s = str.toCharArray().find { !firstCharPattern.matcher(it.toString()).find() }
        return if (upperCase) {
            s?.uppercaseChar()
        } else {
            s
        }?.toString() ?: ""
    }

    fun getRandomBackgroundTint() = ColorStateList.valueOf(getRandomColor())

    private val firstCharPattern =
        Pattern.compile("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]$")

    fun getAppTitle(context: Context, view: View): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        getAppName(context)?.run {
            sb.append(this)
            setAppEggTitleSuffix(sb, view)
        }
        return sb
    }
}