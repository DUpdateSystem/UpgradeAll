package net.xzos.upgradeall.utils.egg

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.module.network.OkHttpApi
import net.xzos.upgradeall.utils.MiscellaneousUtils
import org.json.JSONException
import org.json.JSONObject
import java.util.*

private const val TAG = "Egg"
private val logObjectTag = ObjectTag("UI", TAG)

private val eggDay by lazy { runBlocking(Dispatchers.Default) { getEggDayOrNull() } }

fun getEggDayOrNull(): Day? {
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DATE)
    return when {
        month == 10 && day == 31 -> Day.HALLOWEEN
        else -> getEggDayOnline()
    }
}

fun getEggDayOnline(): Day? {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DATE)
    val url = "http://timor.tech/api/holiday/info/$year-$month-$day"
    val holidayName: String
    try {
        val json = JSONObject(OkHttpApi.get(logObjectTag, url)?.body?.string() ?: return null)
        val holidayJson = json.getJSONObject("holiday")
        holidayName = holidayJson.getString("name")
    } catch (e: JSONException) {
        Log.e(logObjectTag, TAG, "getEggDay: ${e.stackTraceToString()}")
        return null
    }
    return when (holidayName) {
        "春节" -> Day.CHINESE_NEW_YEAR
        "端午节" -> Day.DRAGON_BOAT_FESTIVAL
        else -> null
    }
}

@Suppress("NON_EXHAUSTIVE_WHEN")
fun egg() {
    when (eggDay) {
        Day.HALLOWEEN -> halloweenAndBirthday()
    }
}

fun halloweenAndBirthday() {
    MiscellaneousUtils.showToast(
        "\uD83C\uDF6C\uD83E\uDD70\uD83D\uDE0B\uD83D\uDE1D\uD83D\uDE09",
        Toast.LENGTH_LONG
    )
}

@Suppress("NON_EXHAUSTIVE_WHEN")
fun setAppEggTitleSuffix(sb: SpannableStringBuilder, view: View) {
    val height: Int = view.height
    val width: Int = view.height
    when (eggDay) {
        Day.CHINESE_NEW_YEAR -> sb.append(getChineseNewYearExtraText())
        Day.DRAGON_BOAT_FESTIVAL -> sb.append(
            " ",
            getImageSpan(context, R.drawable.ic_rice_dumpling, width, height),
            0
        )
    }
}

fun getImageSpan(
    context: Context,
    @DrawableRes resourceId: Int, width: Int, height: Int
): ImageSpan? {
    val drawable = ContextCompat.getDrawable(context, resourceId) ?: return null
    drawable.setBounds(0, 0, width, height)
    return ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
}

fun getChineseNewYearExtraText(): String {
    return "\uD83C\uDFEE"
}