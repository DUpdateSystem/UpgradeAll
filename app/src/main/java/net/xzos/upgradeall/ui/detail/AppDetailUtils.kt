package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun getVersionNameSpannableStringWithRes(
    rawVersionStringList: List<Pair<Char, Boolean>>,
    @ColorRes normalColorRes: Int?, @ColorRes lowLevelColorRes: Int?, context: Context,
    sb: SpannableStringBuilder = SpannableStringBuilder()
): SpannableStringBuilder {
    val highlightColor = if (normalColorRes != null)
        ContextCompat.getColor(context, normalColorRes)
    else null
    val normalColor = if (lowLevelColorRes != null)
        ContextCompat.getColor(context, lowLevelColorRes)
    else null
    return getVersionNameSpannableString(rawVersionStringList, highlightColor, normalColor, sb)
}

fun getVersionNameSpannableString(
    rawVersionStringList: List<Pair<Char, Boolean>>,
    @ColorInt normalColor: Int?, @ColorInt lowLevelColor: Int?,
    sb: SpannableStringBuilder = SpannableStringBuilder()
): SpannableStringBuilder {
    rawVersionStringList.forEach {
        setSpannableStringBuilderColor(
            it.first.toString(), sb, if (it.second) normalColor else lowLevelColor
        )
    }
    return sb
}

private fun setSpannableStringBuilderColor(
    s: String, sb: SpannableStringBuilder, @ColorInt color: Int?
) {
    sb.append(s)
    if (color != null) {
        val newLength = sb.length
        sb.setSpan(
            ForegroundColorSpan(color), newLength - s.length, newLength,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
