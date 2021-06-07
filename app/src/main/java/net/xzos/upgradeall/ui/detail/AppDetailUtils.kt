package net.xzos.upgradeall.ui.detail

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import net.xzos.upgradeall.R

fun getVersionNameSpannableStringWithRes(
    rawVersionStringList: List<Pair<Char, Boolean>>,
    @ColorRes highlightResColor: Int?,
    context: Context,
    sb: SpannableStringBuilder = SpannableStringBuilder()
): SpannableStringBuilder {
    return getVersionNameSpannableString(
        rawVersionStringList, if (highlightResColor != null)
            ContextCompat.getColor(context, highlightResColor) else null,
        context, sb
    )
}

fun getVersionNameSpannableString(
    rawVersionStringList: List<Pair<Char, Boolean>>,
    @ColorInt highlightColor: Int?,
    context: Context,
    sb: SpannableStringBuilder = SpannableStringBuilder()
): SpannableStringBuilder {
    rawVersionStringList.forEach {
        setVersionNumberSpannableStringBuilder(
            it.first.toString(), sb, it.second, highlightColor,
            context
        )
    }
    return sb
}

private fun setVersionNumberSpannableStringBuilder(
    s: String, sb: SpannableStringBuilder,
    focus: Boolean = false, focusColor: Int? = null,
    context: Context
) {
    sb.append(s)
    val color = when {
        !focus -> ContextCompat.getColor(context, R.color.text_low_priority_color)
        focus && focusColor != null -> focusColor
        else -> null
    }
    color?.run {
        val newLength = sb.length
        sb.setSpan(
            ForegroundColorSpan(this), newLength - s.length, newLength,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

    }
}
